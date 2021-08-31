package net.whydah.sso.useradmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Password management self service.
 */
@Controller
public class PasswordChangeController {
	private static final Logger log = LoggerFactory.getLogger(PasswordChangeController.class);
	private static final Client uasClient = Client.create();
	private URI uasServiceUri;


	public PasswordChangeController() throws IOException {
		uasServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("useradminservice")).build();
	}

	@RequestMapping("/test/*")
	public String t1(HttpServletRequest request) {
		String path = request.getPathInfo();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	@RequestMapping("/resetpassword")
	public String resetpassword(HttpServletRequest request, HttpServletResponse response, Model model) {
		log.trace("resetpassword was called");
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.setCSP(response);

		String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
		model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);

		if (request.getParameter("username") == null || request.getParameter("username").length() < 3) {
			return "resetpassword";
		}
		if (!UserName.isValid(request.getParameter("username"))) {
			model.addAttribute("error", "\nIllegal username");
			return "resetpassword";
		}
		
		String username = new UserName(request.getParameter("username")).getInput();

		WebResource uasWR = uasClient.resource(uasServiceUri).path(SessionDao.instance.getServiceClient().getMyAppTokenID() + "/auth/password/reset/username/" + username);
		ClientResponse uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
		if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
			String error = uasResponse.getEntity(String.class);
			log.error(error);
			model.addAttribute("error", error + "\nusername:" + username);
			return "resetpassword";
		} else {
			if (redirectURI != null && redirectURI.length() > 10) {
				SessionDao.instance.addRedirectURIForNewUser(username, redirectURI);        	
			}
		}
		return "resetpassworddone";
	}

	@RequestMapping("/changepassword/*")
	public String changePasswordFromLink(HttpServletRequest request, HttpServletResponse response, Model model) {
		log.warn("changePasswordFromLink was called");
		PasswordChangeToken passwordChangeToken = getTokenFromPath(request);
		//model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
		SessionDao.instance.addModel_LOGO_URL(model);
		//model.addAttribute(ConstantValue.MYURI, properties.getProperty("myuri"));
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.setCSP(response);

		String redirectURI = SessionDao.instance.getRedirectURIForNewUser(passwordChangeToken.getUser());
		if(redirectURI!=null) {
			model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
		}

		model.addAttribute("username", passwordChangeToken.getUser());
		model.addAttribute("token", passwordChangeToken.getToken());
		if (!passwordChangeToken.isValid()) {
			return "changepasswordtokentimedout";
		} else {
			return "changepassword";
		}
	}

	@RequestMapping("/dochangepassword/*")
	public String doChangePasswordFromLink(HttpServletRequest request, HttpServletResponse response, Model model) {
		log.trace("doChangePasswordFromLink was called");
		SessionDao.instance.addModel_LOGO_URL(model);

		String redirectURI =  SessionDao.instance.getFromRequest_RedirectURI(request);
		if(redirectURI!=null) {
			model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
		}

		if (!SessionDao.instance.validCSRFToken(SessionDao.instance.getfromRequest_CSRFtoken(request))) {
			log.warn("action - CSRFtoken verification failed. Redirecting to login.");
			model.addAttribute(ConstantValue.PASSWORD_CHANGE_ERROR, "Could not change password - CSRFtoken missing or incorrect");
	
			SessionDao.instance.addModel_LOGO_URL(model);
			SessionDao.instance.addModel_CSRFtoken(model);
	        SessionDao.instance.setCSP(response);
	        SessionDao.instance.addModel_MYURI(model);
	        SessionDao.instance.addModel_WHYDAH_VERSION(model);        
			SessionDao.instance.addModel_LoginTypes(model);
			CookieManager.clearUserTokenCookies(request, response);
			return "/sso/login";

		}
		
		SessionDao.instance.addModel_CSRFtoken(model);
		
		PasswordChangeToken passwordChangeToken = getTokenFromPath(request);
		String newpassword = request.getParameter("newpassword");
		//        WebResource uibWR = uibClient.resource(uibServiceUri).path("/password/" + tokenServiceClient.getMyAppTokenID() + "/reset/username/" + passwordChangeToken.getUser() + "/newpassword/" + passwordChangeToken.getToken());
		WebResource uasWR = uasClient.resource(uasServiceUri).path(SessionDao.instance.getServiceClient().getMyAppTokenID() + "/auth/password/reset/username/" + passwordChangeToken.getUser() + "/newpassword/" + passwordChangeToken.getToken());
		log.trace("doChangePasswordFromLink was called. Calling UAS with url " + uasWR.getURI());

		ClientResponse uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, "{\"newpassword\":\"" + newpassword + "\"}");
		model.addAttribute("username", passwordChangeToken.getUser());
		if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
			String error = uasResponse.getEntity(String.class);
			log.error(error);
			if (uasResponse.getStatus() == ClientResponse.Status.NOT_ACCEPTABLE.getStatusCode()) {
				model.addAttribute("error", "The password you entered was found to be too weak, please try another password.");
			} else {
				model.addAttribute("error", error + "\nusername:" + passwordChangeToken.getUser());
			}
			model.addAttribute("token", passwordChangeToken.getToken());
			return "changepassword";
		}

		//try to log the user on
		UserCredential user = new UserNameAndPasswordCredential(passwordChangeToken.getUser(), newpassword);
		String userTicket = UUID.randomUUID().toString();
		String userTokenXml = SessionDao.instance.getServiceClient().getUserToken(user, userTicket);
        //attach to browser's cookie
		String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);
		
		if(redirectURI!=null && !redirectURI.equals("welcome")) {			
			if (userTokenXml != null) {
				redirectURI = SessionDao.instance.getServiceClient().appendTicketToRedirectURI(redirectURI, userTicket);    		
			}
		} else {
			redirectURI = "/sso/welcome";
		}


		model.addAttribute(ConstantValue.REDIRECT, redirectURI);
		log.info("login - Redirecting to {}", redirectURI);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "action";
	}

	private PasswordChangeToken getTokenFromPath(HttpServletRequest request) {
		String path = request.getPathInfo();
		String tokenString = path.substring(path.lastIndexOf('/') + 1);
		return new PasswordChangeToken(tokenString);
	}

	public static String sanitize(String string) {
		if (string == null || string.length() < 3) {
			return string;
		}
		return string
				.replaceAll("(?i)%3c%2fnoscript%3e", "")   // case 1
				.replaceAll("(?i)%2fscript%3e", "")   // case 1
				.replaceAll("(?i)<script.*?>.*?</script.*?>", "")   // case 1
				.replaceAll("(?i)<.*?javascript:.*?>.*?</.*?>", "") // case 2
				.replaceAll("(?i)<.*?\\s+on.*?>.*?</.*?>", "")     // case 3
				.replaceAll("alert", "")    // alerts
				.replaceAll("prompt", "")    // prompt
				.replaceAll("confirm", "");  // confirms
	}


	public static String sanitizeUsername(String string) {
		if (string == null || string.length() < 3) {
			return string;
		}
		return string
				.replaceAll("(?i)%3c%2fnoscript%3e", "")   // case 1
				.replaceAll("(?i)%2fscript%3e", "")   // case 1
				.replaceAll("(?i)<script.*?>.*?</script.*?>", "")   // case 1
				.replaceAll("(?i)<.*?javascript:.*?>.*?</.*?>", "") // case 2
				.replaceAll("(?i)<.*?script:.*?>.*?</.*?>", "") // case 2
				.replaceAll("(?i)<.*?\\s+on.*?>.*?</.*?>", "")     // case 3
				.replaceAll("alert", "")    // alerts
				.replaceAll("prompt", "")    // prompt
				.replaceAll("confirm", "");  // confirms
	}

}

