package net.whydah.sso.authentication.oidc;

import com.nimbusds.oauth2.sdk.GeneralException;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class LoginController {

	private final static Logger log = LoggerFactory.getLogger(LoginController.class);
	protected final String logoUrl;
	private final WhydahServiceClient tokenServiceClient;
	private final String appId;
	private final String provider;
	private final String service;
	//private final String loginEnabled;
	private final boolean loginEnabled;
	private final AuthHelper authHelper;
	private final SessionManagementHelper sessionManagementHelper;
	private final String welcomeURL;

	public LoginController(String provider, String logoUrl, String issuerUrl, String appId, String appSecret, String appUri, boolean enabled) throws GeneralException, IOException, URISyntaxException {
		this.provider = provider;
		this.service = "oidcProvider"+provider.substring(0, 1).toUpperCase() + provider.substring(1);
		//this.loginEnabled = provider+".enabled";
		this.loginEnabled = enabled;
		this.tokenServiceClient = SessionDao.instance.getServiceClient();
		this.logoUrl = logoUrl;
		this.appId = appId;
		this.sessionManagementHelper = new SessionManagementHelper(provider);
		if (this.loginEnabled) {
			this.authHelper = new AuthHelper(appUri, provider, issuerUrl, appId, appSecret, new String[]{"openid", "name", "phoneNumber", "email", "profile"}, this.sessionManagementHelper);
			SessionDao.instance.addOIDCProvider(provider);
		} else {
			this.authHelper = null;
		}
		if (appUri.charAt(appUri.length()-1) == '/') {
			welcomeURL = appUri + "welcome";
		} else {
			welcomeURL = appUri + "/welcome";
		}
	}

	//@RequestMapping("/login")
	public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		log.trace(provider + "login  start");
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		model.addAttribute(ConstantValue.LOGO_URL, logoUrl);
		if (!loginEnabled) {
		//if (!SessionDao.instance.isLoginTypeEnabled(loginEnabled)) {
			log.trace(provider + "login  return login");
			return "login";
		}
		if (!authHelper.isAuthenticated(httpRequest)) {
			String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}
		
		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				ex.printStackTrace();
				log.warn("Cannot refresh token from  provider. Return login");
				String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
				model.addAttribute("redirect", aad_auth_url);
				log.info("Redirecting to {}", aad_auth_url);
				return "action";
			}
		}
		//return redirectURi with a ticket
		log.info("login resolve");
		return resolve(httpRequest, httpResponse, model, httpRequest.getParameter("redirectURI"));
	}

	//@RequestMapping("/auth")
	public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		log.info("auth  start");
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!loginEnabled) {
		//if (!SessionDao.instance.isLoginTypeEnabled(loginEnabled)) {
			return "login";
		}
		//get the original redirectURI
		String redirectURI = authHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(httpRequest);
		//append the ticket and return to our client
		return resolve(httpRequest, httpResponse, model, redirectURI);
	}

	private String resolve(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, String redirectURI) throws Exception {
		log.info("auth resolve");
		if (redirectURI == null || redirectURI.contentEquals("") || redirectURI.equalsIgnoreCase("welcome")) {
			redirectURI = welcomeURL;
		}

		String firstName = sessionManagementHelper.getFirstName(httpRequest); //(String) payload.get("given_name");
		String lastName = sessionManagementHelper.getLastName(httpRequest); //(String) payload.get("family_name");
		String email = sessionManagementHelper.getEmail(httpRequest);
		String phoneNumber = sessionManagementHelper.getPhoneNumber(httpRequest);
		String subject = sessionManagementHelper.getSubject(httpRequest);

		String userticket = UUID.randomUUID().toString();

		UserCredential credential = new UserCredential(email, subject);
		log.info("UserCredential credential {}", credential);

		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
		log.info("userTokenXml(1):" + userTokenXml);
		if (userTokenXml == null) {
			//corner case: username is prepended with the 3rd party marker
			credential = new UserCredential(provider+"-" + email, subject);
			log.info("UserCredential(2) credential:" + credential);
			userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
			log.info("userTokenXml(2):" + userTokenXml);
		}

		if (userTokenXml == null) {
			log.info("userTokenXml(3)");

			//register a cookie that contains sessionid which can be used on other server instances
			registerAuthCookie(httpRequest, httpResponse);

			if (!SessionDao.instance.checkIfUserExists(email)) {
				//we should ask user to confirm their info and give their consents as follows
				log.info("return toBasicInfoConfirm");
				return toBasicInfoConfirm(model, redirectURI, email, firstName, lastName, email, phoneNumber, false, false);
			} else {
				//prompt "username exists. Is it you? Yes/No
				//if Yes, enter username/password. Otherwise, append the 3rd party marker to the username
				log.info("return toCredentialConfirm");

				return toCredentialConfirm(model, redirectURI, email, null );
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
		1. SSOLWA-1 handles the  redirect call and establishes a session (sessonid, sessiondata) and calls the "confirm" page
		2. The POST "confirm" request is then handled in SSOLWA-2 - the one has no knowledge of the current session that SSOLWA-1 is processing
	 */
	private void registerAuthCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String sessionid = httpRequest.getSession().getId();
		SessionCookieHelper.addSessionCookie("", sessionid, httpResponse);
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
		model.addAttribute("service", this.service);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);

		
		if(!userTokenFound) {
			//register process, we show consents here
		}
		return "confirm_basicinfo";
	}

	private String toCredentialConfirm(Model model, String redirectURI, String username, String confirmError) {
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("service", this.service);
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
		return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket));
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

	//@RequestMapping("/basicinfo_confirm")
	public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!loginEnabled) {
			return "login";
		}
		if (!authHelper.isAuthenticated(httpRequest)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate
			String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
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
		if (redirectURI == null || redirectURI.isEmpty() || redirectURI.equalsIgnoreCase("welcome")) {
			redirectURI = welcomeURL;
		}

		//AuthResult auth = SessionManagementHelper.getAuthSessionObject(httpRequest);
		String stored_email = sessionManagementHelper.getEmail(httpRequest);
		String stored_subject = sessionManagementHelper.getSubject(httpRequest);
		String stored_accessToken = sessionManagementHelper.getAccessToken(httpRequest);


		if(userName==null || (!userName.equalsIgnoreCase(stored_email) && !userName.equalsIgnoreCase(provider+"-" + stored_email)  )) {
			//misuse - illegal access check
			return toLogin(model, redirectURI, "illegal username");
		}
		if(firstName ==null || firstName.isEmpty()) {
			return toLogin(model, redirectURI, "illegal first name");
		}
		if(lastName ==null || lastName.isEmpty()) {
			return toLogin(model, redirectURI, "illegal last name");
		}
		if(email ==null || email.isEmpty()) {
			return toLogin(model, redirectURI, "illegal email");
		}
		if(cellPhone ==null || cellPhone.isEmpty()) {
			return toLogin(model, redirectURI, "illegal cell phone");
		}
		
		
		UserCredential credential = new UserCredential(userName, stored_subject);
		String userticket = UUID.randomUUID().toString();
		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);

		if (userTokenXml == null) {	
			String personRef = UUID.randomUUID().toString();
			userTokenXml = tokenServiceClient.createAndLogonUser("", stored_accessToken, null, stored_subject, userName, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);
		} 


		if(userTokenXml!=null) {
			
			String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
			String uid = UserTokenXpathHelper.getUserID(userTokenXml);
			String username = UserTokenXpathHelper.getUserName(userTokenXml);
			
			setWhydahCookie(httpRequest, httpResponse, userTokenXml);
			updateLifeSpanForAuthSession(httpRequest, userTokenXml);


			SessionDao.instance.saveRoleDatatoWhydah(uid, appId, provider, "Whydah", "PersonId", personRef);
			SessionDao.instance.syncWhydahUserInfoWithThirdPartyUserInfo(UserTokenXpathHelper.getUserID(userTokenXml), "", stored_accessToken, resolveAppRoles(null), stored_subject, username, firstName, lastName, email, cellPhone, personRef);


			return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket));
		} else {

			return toLogin(model, redirectURI, "failure solving usertokenxml");
		}

	}

	private void updateLifeSpanForAuthSession(HttpServletRequest httpRequest, String userTokenXml) {
		Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
		Long tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
		Long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;

		//update the lifespance of the current session if existing in the map and cookie expirattion date
		sessionManagementHelper.updateLifeSpanForSession(httpRequest, endOfTokenLifeMs);
	}

	private String resolveAppRoles(String approles) {

		if(approles==null) {
			approles="User";
		}
		return approles;
	}

	//@RequestMapping("/credential_confirm")
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			return "login";
		}

		if (!authHelper.isAuthenticated(httpRequest)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate

			String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
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

			String firstName= sessionManagementHelper.getFirstName(httpRequest);//(String) payload.get("given_name");
			String lastName = sessionManagementHelper.getLastName(httpRequest);//(String) payload.get("family_name");
			String phoneNumber = sessionManagementHelper.getPhoneNumber(httpRequest);
			String email = sessionManagementHelper.getEmail(httpRequest);

			return toBasicInfoConfirm(model, redirectURI, provider+"-" + email, firstName, lastName, email, phoneNumber, false, true);

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
	/*
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.GOOGLELOGIN_ENABLED)) {
			return "login";
		}

		if (!authHelper.isAuthenticated(httpRequest)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate

			String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.info("isAccessTokenExpired  {}", httpRequest);
				//authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch(Exception ex) {
				String aad_auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
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
			if (SessionDao.instance.checkIfUserExists(userName)) {
				return toCredentialConfirm(model, redirectURI, userName, "Username already existed!");
			} else {
				String firstName = sessionManagementHelper.getFirstName(httpRequest);//(String) payload.get("given_name");
				String lastName = sessionManagementHelper.getLastName(httpRequest);//(String) payload.get("family_name");
				String email = sessionManagementHelper.getEmail(httpRequest);

				//return toBasicInfoConfirm(model, redirectURI, userName, firstName, lastName, email, null, false, true);
				return toBasicInfoConfirm(model, redirectURI, "google-" + email, firstName, lastName, email, null, false, true);
			}
		}
		/*
		if(userName!=null && pwd!=null && userName.length()>0 && pwd.length()>0) {
			userTokenXml = tokenServiceClient.getUserToken(new UserCredential(userName, pwd), userticket);
		} 

		if(userTokenXml!=null) {
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml);
		} else {

			String firstName= sessionManagementHelper.getFirstName(httpRequest);//(String) payload.get("given_name");
			String lastName = sessionManagementHelper.getLastName(httpRequest);//(String) payload.get("family_name");
			String email = sessionManagementHelper.getEmail(httpRequest);

			//for now, just prepend the marker -misterhuydo@gmail.com for example
			return toBasicInfoConfirm(model, redirectURI, provider + "-" +  email, firstName, lastName, email, null, false, true);
		}
	}
}
		 */
