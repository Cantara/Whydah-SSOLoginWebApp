package net.whydah.sso.authentication.whydah;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.errorhandling.AppException;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.utils.SignupHelper;

@Controller
public class SSOLoginController {

	private final static Logger log = LoggerFactory.getLogger(SSOLoginController.class);

	public SSOLoginController() throws IOException {

	}

	@RequestMapping("/login")
	public String login(HttpServletRequest request, HttpServletResponse response,Model model) {

		try {

			String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
			boolean sessionCheckOnly = SessionDao.instance.getFromRequest_SessionCheck(request);
			String userTicket = request.getParameter("userticket");

			SessionDao.instance.updateApplinks();
			SessionDao.instance.addModel_LOGO_URL(model);
			SessionDao.instance.addModel_CSRFtoken(model);
			SessionDao.instance.setCSP(response);
			SessionDao.instance.addModel_MYURI(model);
			SessionDao.instance.addModel_WHYDAH_VERSION(model);
			model.addAttribute(ConstantValue.SESSIONCHECK, sessionCheckOnly);
			model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);

			CookieManager.addSecurityHTTPHeaders(response);
			String userTokenId = CookieManager.getUserTokenIdFromCookie(request);

			log.trace("login: redirectURI={}, SessionCheck={}, userTokenIdFromCookie={}", redirectURI, sessionCheckOnly, userTokenId);


			String userTokenXml = null;
			if (userTicket != null) {
				userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTicket(userTicket);
				if (userTokenXml != null) {
					userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
					log.debug("User ticket found, override with new user token id");
				} else {
					log.debug("User ticket not found");
				}
			}

			if (shouldClearUserToken(userTokenId)) {
				userTokenId = clearUserToken(request, response, userTokenId); // Will set userTokenId to null
			}

			if (userTokenId != null) {
				if (userTokenXml == null) {
					userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
				}

				if(userTokenXml!=null) {
					storeUserTokenInCookie(request, response, userTokenXml);

					log.debug("login - redirecting request");

					if (ConstantValue.DEFAULT_REDIRECT.equalsIgnoreCase(redirectURI)) {
						log.trace("login - Did not find any sensible redirectURI, using /welcome");
						model.addAttribute(ConstantValue.REDIRECT, redirectURI);
						SessionDao.instance.addModel_CSRFtoken(model);
						log.info("login - Redirecting to {}", redirectURI);
						return "action";
					} else {
						log.info("login - created new userticket={} for usertokenid={}", userTicket, userTokenId);

						if (SessionDao.instance.getServiceClient().createTicketForUserTokenID(userTicket, userTokenId)) {
							redirectURI = prependTicketToRedirectURI(redirectURI, userTicket);
							log.debug("Created a new user ticket [userTicket={}, userTokenId={}]", userTicket, userTokenId);
						} else {
							log.warn("Can not create ticket [userTokenId={}]", userTokenId);
						}

						String referer_channel = request.getParameter("referer_channel");
						if(referer_channel!=null) {
							redirectURI = prependRefererToRedirectURI(redirectURI, referer_channel);
						}
						redirectURI = prependRefererToRedirectURI(redirectURI, userTicket);
						redirectURI = appendHttpSchemeIfNotFound(redirectURI);
						model.addAttribute(ConstantValue.REDIRECT, redirectURI);
						log.info("login - Redirecting to {}", redirectURI);
						SessionDao.instance.addModel_CSRFtoken(model);
						return "action";
					}   

				}


			}


			// Added return is sessioncheck only and no cookie found
			if (sessionCheckOnly) {
				// Action use redirect - not redirectURI
				redirectURI = appendHttpSchemeIfNotFound(redirectURI);
				model.addAttribute(ConstantValue.REDIRECT, redirectURI);
				log.info("login - isSessionCheckOnly - Redirecting to {}", redirectURI);
				//return Response.ok(FreeMarkerHelper.createBody("/action.ftl", model.asMap())).build();
				return "action";

			} 

			//ModelHelper.setEnabledLoginTypes(model);
			SessionDao.instance.addModel_LoginTypes(model);
			//model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
			SessionDao.instance.addModel_CSRFtoken(model);
		} catch(Exception ex) {
			ex.printStackTrace();
			log.error("unexpected error", ex);
		}
		//return Response.ok(FreeMarkerHelper.createBody("/login.ftl", model.asMap())).build();
		return "login";
	}

	private String appendHttpSchemeIfNotFound(String redirectURI) {
		if(!isDefaultRedirect(redirectURI) && !redirectURI.toLowerCase().startsWith("http")) {
			redirectURI = "https://" + redirectURI;
		}
		return redirectURI;
	}
	private boolean isDefaultRedirect(String redirectURI) {
		return redirectURI.startsWith(SessionDao.instance.DEFAULT_REDIRECT);
	}

	private UserToken storeUserTokenInCookie(HttpServletRequest request,
			HttpServletResponse response, String userTokenXml) {
		UserToken loginUserToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
		Integer tokenRemainingLifetimeSeconds = SessionDao.instance.getServiceClient().calculateTokenRemainingLifetimeInSeconds(userTokenXml);
		CookieManager.createAndSetUserTokenCookie(loginUserToken.getUserTokenId(), tokenRemainingLifetimeSeconds * 1000, request, response);
		return loginUserToken;
	}

	private String clearUserToken(HttpServletRequest request, HttpServletResponse response, String userTokenId) {
		log.info("Clearing user token [userTokenId={}]", userTokenId);
		CookieManager.clearUserTokenCookies(request, response);
		return null;
	}

	private boolean shouldClearUserToken(String userTokenId) {
		return isLogoutUserTokenId(userTokenId) || isInvalidUserTokenId(userTokenId);
	}

	private boolean isLogoutUserTokenId(String userTokenId) {
		return "logout".equalsIgnoreCase(userTokenId);
	}

	private boolean isInvalidUserTokenId(String userTokenId) {
		return userTokenId != null && !SessionDao.instance.getServiceClient().verifyUserTokenId(userTokenId);
	}

	String prependTicketToRedirectURI(String redirectURI, String userticket) {
		String[] parts = redirectURI.split("\\?", 2);
		String r ="";
		if(parts.length==2) {
			r = parts[0] + "?userticket=" + userticket + "&" + parts[1]; 
		} else {
			r = parts[0] + "?userticket=" + userticket;
		}
		return r;
	}

	String prependRefererToRedirectURI(String redirectURI, String referer_channel) {
		String[] parts = redirectURI.split("\\?", 2);
		String r ="";
		if(parts.length==2) {
			r = parts[0] + "?referer_channel=" + referer_channel + "&" + parts[1]; 
		} else {
			r = parts[0] + "?referer_channel=" + referer_channel;
		}
		return r;
	}



	@RequestMapping("/welcome")
	public String welcome(HttpServletRequest request, HttpServletResponse response,Model model) {

		String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_WHYDAH_VERSION(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.setCSP(response);


		if (userTokenXml == null) {
			return "login";
		} else {

			boolean sessionCheckOnly = SessionDao.instance.getFromRequest_SessionCheck(request);
			model.addAttribute(ConstantValue.SESSIONCHECK, sessionCheckOnly);
			model.addAttribute(ConstantValue.USERTOKEN, SignupHelper.trim(userTokenXml));
			model.addAttribute(ConstantValue.APP_LINKS, SessionDao.instance.getAppLinks());
			model.addAttribute(ConstantValue.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
			model.addAttribute(ConstantValue.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
			model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
			model.addAttribute(ConstantValue.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
			model.addAttribute(ConstantValue.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
			//SessionDao.instance.getReportServiceHelper().addUserActivities(model, userTokenXml);
			return ConstantValue.DEFAULT_REDIRECT;
		}
	}

	@RequestMapping("/action")
	public String action(HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {

		String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
		log.trace("action: redirectURI: {}", redirectURI);

		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_WHYDAH_VERSION(model);
		SessionDao.instance.addModel_IAM_MODE(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.setCSP(response);
		SessionDao.instance.addModel_LoginTypes(model);

		model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);

		SessionDao.instance.addModel_LoginTypes(model);
		//if (!SessionHelper.validCSRFToken(request.getParameter(SessionHelper.CSRFtoken))) {
		if(!SessionDao.instance.validCSRFToken(SessionDao.instance.getfromRequest_CSRFtoken(request))) {
			log.warn("action - CSRFtoken verification failed. Redirecting to login.");
			model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in - CSRFtoken missing or incorrect");
			//model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
			SessionDao.instance.addModel_CSRFtoken(model);
			//ModelHelper.setEnabledLoginTypes(model);
			SessionDao.instance.addModel_LoginTypes(model);
			CookieManager.clearUserTokenCookies(request, response);
			model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
			return "login";

		}

		if (!UserName.isValid(SessionDao.instance.getFromRequest_User(request)) || !Password.isValid(SessionDao.instance.getFromRequest_Password(request))) {
			log.warn("action - illegal username or password. Redirecting to login.");
			model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in.");
			CookieManager.clearUserTokenCookies(request, response);
			return "login";
		}


		UserCredential user = new UserNameAndPasswordCredential(SessionDao.instance.getFromRequest_User(request), SessionDao.instance.getFromRequest_Password(request));
		String userTicket = UUID.randomUUID().toString();
		String userTokenXml = SessionDao.instance.getServiceClient().getUserToken(user, userTicket);

		if (userTokenXml == null) {
			log.warn("action - getUserToken failed. Redirecting to login.");
			model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in.");
			CookieManager.clearUserTokenCookies(request, response);
			return "login";
		}

		SessionDao.instance.getServiceClient().getWAS().updateDefcon(userTokenXml);
		if (redirectURI.contains(ConstantValue.USERTICKET) && !redirectURI.toLowerCase().contains("http")) {
			log.warn("action - redirectURI contain ticket and no URL. Redirecting to welcome.");
			model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not redirect back, redirect loop detected.");
			//ModelHelper.setEnabledLoginTypes(model);
			SessionDao.instance.addModel_LoginTypes(model);
			model.addAttribute(ConstantValue.REDIRECT_URI, "");
			model.addAttribute(ConstantValue.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
			model.addAttribute(ConstantValue.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
			model.addAttribute(ConstantValue.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
			model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
			model.addAttribute(ConstantValue.DEFCON, SessionDao.instance.getServiceClient().getWAS().getDefcon());
			model.addAttribute(ConstantValue.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
			SessionDao.instance.getCRMHelper().getCrmdata_AddToModel(model, userTokenXml);
			//SessionDao.instance.getReportServiceHelper().addUserActivities(model, userTokenXml);
			return "welcome";
		}

		String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
		Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
		CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

		// ticket on redirect
		if (redirectURI.toLowerCase().contains(ConstantValue.USERTICKET)) {
			// Do not overwrite ticket
		} else {
			redirectURI = SessionDao.instance.getServiceClient().appendTicketToRedirectURI(redirectURI, userTicket);
		}

		// Attempt for a workaround for space instead of + in email seen from invite flows
		try {
			model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
		} catch (Exception e){
			log.error("Exception in email from usertoken xml.getEmail");
			model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml).replace(" ","+"));
		}
		// Action use redirect...
		redirectURI = appendHttpSchemeIfNotFound(redirectURI);
		model.addAttribute(ConstantValue.REDIRECT, redirectURI);
		model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
		log.info("action - Redirecting to {}", redirectURI);
		return "action";
	}

}


