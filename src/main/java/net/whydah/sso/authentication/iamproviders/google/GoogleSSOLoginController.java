package net.whydah.sso.authentication.iamproviders.google;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.whydah.WhydahOAuthSessionManagementHelper;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.user.Email;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;

@Controller
public class GoogleSSOLoginController {

	private final static Logger log = LoggerFactory.getLogger(GoogleSSOLoginController.class);
	protected String LOGOURL = "/sso/images/site-logo.png";
	@Autowired GoogleAuthHelper ggHelper;
	private final WhydahServiceClient tokenServiceClient;// = SessionDao.instance.getServiceClient();
	private String APPID="", APPNAME="";
	
	public GoogleSSOLoginController() throws IOException {
		Properties properties = AppConfig.readProperties();
		tokenServiceClient = SessionDao.instance.getServiceClient();
		LOGOURL = properties.getProperty("logourl");
		APPID = properties.getProperty("applicationid");
		APPNAME = properties.getProperty("applicationname");
	}

	@RequestMapping("/googlelogin")
	public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		log.info("googlelogin  start");
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			log.info("googlelogin  return login");
			return "login";
		}
		if (!ggHelper.isAuthenticated(httpRequest)) {
			String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter(ConstantValue.LOGIN_HINT), httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}
		
		if (ggHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				ggHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				ex.printStackTrace();
				log.warn("Cannot refresh token from Google provider. Return login");
				String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter(ConstantValue.LOGIN_HINT), httpRequest.getParameter("redirectURI"));
				model.addAttribute("redirect", aad_auth_url);
				log.info("Redirecting to {}", aad_auth_url);
				return "action";
			}
		}
		//return redirectURi with a ticket
		log.info("googlelogin resolve");
		return resolve(httpRequest, httpResponse, model, httpRequest.getParameter("redirectURI"));
	}

	@RequestMapping("/googleauth")
	public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		log.info("googleauth  start");
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			return "login";
		}
		//get the original redirectURI
		String redirectURI = ggHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(httpRequest);
		//append the ticket and return to our client
		return resolve(httpRequest, httpResponse, model, redirectURI);
	}

	private String resolve(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, String redirectURI) throws Exception {
		log.info("googleauth resolve");
		if (redirectURI == null || redirectURI.contentEquals("")) {
			redirectURI = "welcome";
		}

		//info from 3rd party
		//GoogleAuthResult auth = GoogleSessionManagementHelper.getAuthSessionObject(httpRequest);
		//Payload payload = auth.getIdToken().getPayload();
		/*
		String userId = payload.getSubject();  // Use this value as a key to identify a user.
		String email = payload.getEmail();
		boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
		String name = (String) payload.get("name");
		String pictureUrl = (String) payload.get("picture");
		String locale = (String) payload.get("locale");
		String familyName = (String) payload.get("family_name");
		String givenName = (String) payload.get("given_name");
		 */
		String firstName = GoogleSessionManagementHelper.getFirstName(httpRequest); //(String) payload.get("given_name");
		String lastName = GoogleSessionManagementHelper.getLastName(httpRequest); //(String) payload.get("family_name");
		String email = GoogleSessionManagementHelper.getEmail(httpRequest); 
		String subject = GoogleSessionManagementHelper.getSubject(httpRequest); 

		String userticket = UUID.randomUUID().toString();

		GoogleUserCredential credential = new GoogleUserCredential(subject, email, subject);
		log.info("GoogleUserCredential credential {}", credential);

		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
		log.info("userTokenXml(1):" + userTokenXml);
		if (userTokenXml == null) {
			//corner case: username is prepended with the 3rd party marker
			credential = new GoogleUserCredential(subject, "google-" + email, subject);
			log.info("GoogleUserCredential(2) credential:" + credential);
			userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
			log.info("userTokenXml(2):" + userTokenXml);
		}

		if (userTokenXml == null) {
			log.info("userTokenXml(3)" + userTokenXml);

			//register a cookie that contains sessionid which can be used on other server instances
			registerAuthCookie(httpRequest, httpResponse);

			if (!SessionDao.instance.checkIfUserExists(email)) {
				//we should ask user to confirm their info and give their consents as follows
				log.info("return toBasicInfoConfirm");
				return toBasicInfoConfirm(model, redirectURI, email, firstName, lastName, email, null, false, false);
			} else {
				//prompt "username exists. Is it you? Yes/No
				//if Yes, enter username/password. Otherwise, append the 3rd party marker to the username
				//log.info("return toCredentialConfirm");

				//return toCredentialConfirm(model, redirectURI, email, null );
				
				log.info("provider {} - Found username {} exists", "google", email);
				//try if a session existing for this trusted client
				userTokenXml = tokenServiceClient.logOnBySharedSecrect(email, userticket);
				if(userTokenXml!=null) {
					return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket,
							userTokenXml);
				} else {
					//should not occur here
					//TODO: maybe we redirect the user to the landing page
					return toCredentialConfirm(model, redirectURI, email, null );
				}
				
				
			}

		} else {
			log.info("userTokenXml(4):" + userTokenXml);
			log.info("return confirmUserInfoCheckAndReturn");
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml);
		}
	}

	//add this session to cookie, which can be used on other server instances
	/*
	 * 
	 *  Imagine we fall into this scenario
		1. SSOLWA-1 handles the google redirect call and establishes a session (sessonid, sessiondata) and calls the "confirm" page
		2. The POST "confirm" request is then handled in SSOLWA-2 - the one has no knowledge of the current session that SSOLWA-1 is processing
	 */
	private void registerAuthCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String sessionid = httpRequest.getSession().getId();
		SessionCookieHelper.addSessionCookie("google", sessionid, httpResponse);
	}

	private String toBasicInfoConfirm(Model model, String redirectURI, String username, String firstName, String lastName, String email, String cellPhone, boolean userTokenFound, boolean newRegister) {
		model.addAttribute("newRegister", String.valueOf(newRegister));
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("slackuser", "");
		model.addAttribute("email", email!=null?email:"");
		model.addAttribute("cellPhone", cellPhone!=null?cellPhone:"");
		model.addAttribute("firstName", firstName!=null?firstName:"");
		model.addAttribute("lastName", lastName!=null?lastName:"");
		model.addAttribute("service", "google");
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);

		
		if(!userTokenFound) {
			//register process, we show consents here
		}
		return "confirm_basicinfo";
	}

	private String extractDomain(String username) {
		String domain = null;
		if(username.contains("@")) {
			domain = username.split("@")[1];
		}
		return domain;
	}

	private String toCredentialConfirm(Model model, String redirectURI, String username, String confirmError) {
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("service", "google");
		if(confirmError!=null && !confirmError.isEmpty()) {
			model.addAttribute("confirmError", confirmError);
		}
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "confirm_credential";
	}

	private String confirmUserInfoCheckAndReturn(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, Model model, String redirectURI, String userticket, String userTokenXml) {
		
		updateLifeSpanForAuthSession(httpRequest, userTokenXml);
		setWhydahCookie(httpRequest, httpResponse, userTokenXml);
		
		String email = UserTokenXpathHelper.getEmail(userTokenXml);
		String domain = extractDomain(email);
		if(domain!= null && !domain.equals("gmail.com")) {
			return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket) + "&referer_channel=" + domain);
		} else {
			return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket));
		}
		
	}

	private void setWhydahCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String userTokenXml) {
		String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);        
		Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
		CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, httpRequest, httpResponse);
	}

	private String toLogin(Model model, String redirectURI, String error) {
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("loginError", "Login error: Could not create or authenticate user due to " + error);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "login";
	}

	private String toAction(Model model, String redirectURI) {
		log.info("Redirecting to {}", redirectURI);
		model.addAttribute("redirect", redirectURI);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "action";
	}

	@RequestMapping("/google_basicinfo_confirm")
	public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			return "login";
		}
		if (!ggHelper.isAuthenticated(httpRequest)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate
			String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (ggHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				ggHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
				return toAction(model, aad_auth_url);
			}
		}

		String userName = httpRequest.getParameter("username");
		String firstName = httpRequest.getParameter("firstName");
		String lastName = httpRequest.getParameter("lastName");
		String email = httpRequest.getParameter("email");
		String cellPhone = httpRequest.getParameter("cellPhone");
		if (cellPhone != null) {
			cellPhone = cellPhone.replaceAll(" ", "").trim();
			if (cellPhone.length() == 8) {
				cellPhone = "+47" + cellPhone;
			}
		}
		String redirectURI = httpRequest.getParameter("redirectURI");
		String slackuser = httpRequest.getParameter("slackuser");

		if (slackuser == null || slackuser.isEmpty()) {
			slackuser = "unknown";
		}
		if (redirectURI == null || redirectURI.isEmpty()) {
			redirectURI = "welcome";
		}

		//GoogleAuthResult auth = GoogleSessionManagementHelper.getAuthSessionObject(httpRequest);
		String stored_email = GoogleSessionManagementHelper.getEmail(httpRequest);
		String stored_subject = GoogleSessionManagementHelper.getSubject(httpRequest);
		String stored_accessToken = GoogleSessionManagementHelper.getAccessToken(httpRequest);


		if(userName==null || (!userName.equalsIgnoreCase(stored_email) && !userName.equalsIgnoreCase("google-" + stored_email)  )) {
			//misuse - illegal access check
			return toLogin(model, redirectURI, "illegal username");
		}
		if(firstName ==null || firstName.isEmpty()) {
			return toLogin(model, redirectURI, "illegal first name");
		}
		if(lastName ==null || lastName.isEmpty()) {
			return toLogin(model, redirectURI, "illegal last name");
		}
		if(email ==null || email.isEmpty() || !Email.isValid(email)) {
			return toLogin(model, redirectURI, "illegal email");
		}
		if(cellPhone ==null || cellPhone.isEmpty()) {
			return toLogin(model, redirectURI, "illegal cell phone");
		}
		
		
		GoogleUserCredential credential = new GoogleUserCredential(stored_subject, userName, stored_subject);
		String userticket = UUID.randomUUID().toString();
		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);

		if (userTokenXml == null) {	
			String personRef = UUID.randomUUID().toString();
			userTokenXml = tokenServiceClient.createAndLogonUser("google", stored_accessToken, null, "", stored_subject, userName, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);				
		} 


		if(userTokenXml!=null) {
			
			String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
			String uid = UserTokenXpathHelper.getUserID(userTokenXml);
			String username = UserTokenXpathHelper.getUserName(userTokenXml);
			
			setWhydahCookie(httpRequest, httpResponse, userTokenXml);
			updateLifeSpanForAuthSession(httpRequest, userTokenXml);


			SessionDao.instance.saveRoleDatatoWhydah(uid, "PersonId", personRef);
			SessionDao.instance.syncWhydahUserInfoWithThirdPartyUserInfo(UserTokenXpathHelper.getUserID(userTokenXml), "google", stored_accessToken, resolveAppRoles(null), "", stored_subject, username, firstName, lastName, email, cellPhone, personRef);

			String domain = extractDomain(stored_email);
			if(domain!= null && !domain.equals("gmail.com")) {
				return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket) + "&referer_channel=" + domain);
			} else {
				return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket));
			}
		} else {

			return toLogin(model, redirectURI, "failure solving usertokenxml");
		}

	}

	private void updateLifeSpanForAuthSession(HttpServletRequest httpRequest, String userTokenXml) {
		Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
		Long tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
		Long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;

		//update the lifespance of the current session if existing in the map and cookie expirattion date
		GoogleSessionManagementHelper.updateLifeSpanForSession(httpRequest, endOfTokenLifeMs);
	}

	private String resolveAppRoles(String approles) {

		if(approles==null) {
			approles="User";
		}
		return approles;
	}

	@RequestMapping("/google_credential_confirm")
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			return "login";
		}

		if (!ggHelper.isAuthenticated(httpRequest)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate

			String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (ggHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				ggHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				String aad_auth_url = ggHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
				return toAction(model, aad_auth_url);
			}
		}

		String newRegister = httpRequest.getParameter("newRegister");
		String userName = httpRequest.getParameter("username");
		String pwd = httpRequest.getParameter("password");
		String redirectURI = httpRequest.getParameter("redirectURI");
		String userTokenXml =null;
		String userticket = UUID.randomUUID().toString();

		if(newRegister.equalsIgnoreCase("false")) {
			if(userName!=null && pwd!=null && userName.length()>0 && pwd.length()>0) {
				userTokenXml = tokenServiceClient.getUserToken(new UserCredential(userName, pwd), userticket);
			} 
			if(userTokenXml == null) {
				return toCredentialConfirm(model, redirectURI, userName, "Login failed. Wrong password!");	
			} else {
				return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml);
			}
		} else {
			
			String firstName= GoogleSessionManagementHelper.getFirstName(httpRequest);//(String) payload.get("given_name");
			String lastName = GoogleSessionManagementHelper.getLastName(httpRequest);//(String) payload.get("family_name");
			String email = GoogleSessionManagementHelper.getEmail(httpRequest);
			
			return toBasicInfoConfirm(model, redirectURI, "google-" + email, firstName, lastName, email, null, false, true);

		}
		
		/*
		if(userName!=null && pwd!=null && userName.length()>0 && pwd.length()>0) {
			userTokenXml = tokenServiceClient.getUserToken(new UserCredential(userName, pwd), userticket);
		} 

		if(userTokenXml!=null) {
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml);
		} else {
			
			String firstName= GoogleSessionManagementHelper.getFirstName(httpRequest);//(String) payload.get("given_name");
			String lastName = GoogleSessionManagementHelper.getLastName(httpRequest);//(String) payload.get("family_name");
			String email = GoogleSessionManagementHelper.getEmail(httpRequest);

			//for now, just prepend the marker google-misterhuydo@gmail.com for example
			return toBasicInfoConfirm(model, redirectURI, "google-" +  email, firstName, lastName, email, null, false);
		}*/
	}
}
