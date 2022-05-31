package net.whydah.sso.authentication.iamproviders.whydah;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.utils.AES;

@Controller
public class WhydahOAuthSSOLoginController {

	private final static Logger log = LoggerFactory.getLogger(WhydahOAuthSSOLoginController.class);
	protected String LOGOURL = "/sso/images/site-logo.png";
	@Autowired WhydahOAuthHelper whydahOauthHelper;
	private final WhydahServiceClient tokenServiceClient = SessionDao.instance.getServiceClient();
	//protected String SECRETKEY = "MYKEYPASSWORD";
	private String APPID="", APPNAME="";
	
	public WhydahOAuthSSOLoginController() throws IOException {
		Properties properties = AppConfig.readProperties();
		LOGOURL = properties.getProperty("logourl");
		
		APPID = properties.getProperty("applicationid");
		APPNAME = properties.getProperty("applicationname");
	}
	
	@RequestMapping("/whydahlogin/{oauth2_provider}")
	public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model,
			@PathVariable("oauth2_provider") String oauth2Provider) throws Throwable {
		log.info("whydahlogin  start - oauth2 provider {}", oauth2Provider);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
		if(!whydahOauthHelper.getProviders().containsKey(oauth2Provider) || !whydahOauthHelper.getProviders().get(oauth2Provider).isEnabled()) {
			log.info("whydahlogin return login");
			return "login";
		}
		
		if (!whydahOauthHelper.isAuthenticated(httpRequest, oauth2Provider)) {
			String aad_auth_url = whydahOauthHelper.getAuthRedirect(httpRequest, oauth2Provider, httpRequest.getParameter("redirectURI"));
			return toAction(model, aad_auth_url);
		}

		if (whydahOauthHelper.isAccessTokenExpired(httpRequest, oauth2Provider)) {
			log.info("isAccessTokenExpired  {}", httpRequest);
			whydahOauthHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse, oauth2Provider);
		}
		//return redirectURi with a ticket
		log.info("whydahlogin resolve");
		return resolve(httpRequest, httpResponse, model, httpRequest.getParameter("redirectURI"), oauth2Provider);
	}
	

	@RequestMapping("/whydahauth")
	public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		log.info("whydahauth start");
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		
		//get the original redirectURI
		StateData stateData = whydahOauthHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(httpRequest);
		//append the ticket and return to our client
		return resolve(httpRequest, httpResponse, model, stateData.getRedirectURI(), stateData.getDomain());
	}

	private String resolve(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, String redirectURI, String selectedProvider) throws Exception {
		log.info("whydahauth resolve");
		
		if (redirectURI == null || redirectURI.contentEquals("")) {
			redirectURI = "welcome";
		}
		
		String uid = WhydahOAuthSessionManagementHelper.getUID(httpRequest, selectedProvider);
		String firstName = WhydahOAuthSessionManagementHelper.getFirstName(httpRequest, selectedProvider);
		String lastName = WhydahOAuthSessionManagementHelper.getLastName(httpRequest, selectedProvider);
		String email = WhydahOAuthSessionManagementHelper.getEmail(httpRequest, selectedProvider); 
		String subject = WhydahOAuthSessionManagementHelper.getSubject(httpRequest, selectedProvider);
		String cellPhone = WhydahOAuthSessionManagementHelper.getCellPhone(httpRequest, selectedProvider);
		String pesonRef = WhydahOAuthSessionManagementHelper.getPersonRef(httpRequest, selectedProvider);
		
		String userticket = UUID.randomUUID().toString();

		WhydahOAuthUserCredential credential = new WhydahOAuthUserCredential(uid, subject, uid);
		log.info("WhydahOAuthUserCredential credential {}", credential);

		//already found check
		String userTokenXml = SessionDao.instance.getServiceClient().getUserToken(credential, userticket);
		log.info("userTokenXml(1):" + userTokenXml);
		if (userTokenXml == null) {
			//corner case: username as uid
			credential = new WhydahOAuthUserCredential(uid, uid, uid);
			log.info("WhydahOAuthUserCredential(2) credential:" + credential);
			userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
			if(userTokenXml!=null) {
				//from this token we use its personRef to find a real username
				userticket = UUID.randomUUID().toString();
				String username = UserTokenXpathHelper.getPersonref(userTokenXml);
				credential = new WhydahOAuthUserCredential(uid, username, uid);
				userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
			}
			log.info("userTokenXml(2):" + userTokenXml);
		}


		if (userTokenXml == null) {
			log.info("userTokenXml(3)" + userTokenXml);
			
			//register a cookie that contains sessionid which can be used on other server instances
			registerAuthCookie(httpRequest, httpResponse, selectedProvider);

			if (!SessionDao.instance.checkIfUserExists(subject)) {
				//we should ask user to confirm their info and give their consents as follows
				log.info("return toBasicInfoConfirm");
				return toBasicInfoConfirm(model, redirectURI, selectedProvider, subject, firstName, lastName, email, cellPhone, false, false);
			} else {
				//prompt "username exists. Is it you? Yes/No
				//if Yes, enter username/password. Otherwise, ask them to create a new username
				log.info("return toCredentialConfirm");
				return toCredentialConfirm(model, redirectURI, selectedProvider, subject, null);
			}

		} else {
			log.info("userTokenXml(4):" + userTokenXml);
			log.info("return confirmUserInfoCheckAndReturn");
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml, selectedProvider);
		}
	}

	
	private void registerAuthCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String provider) {
		String sessionid = httpRequest.getSession().getId();
		SessionCookieHelper.addSessionCookie(provider, sessionid, httpResponse);
	}

	private String toBasicInfoConfirm(Model model, String redirectURI, String provider, String username, String firstName, String lastName, String email, String cellPhone, boolean userTokenFound, boolean newRegister) {
		model.addAttribute("newRegister", String.valueOf(newRegister));
		model.addAttribute("whydahOauth2Provider", provider);
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("slackuser", "");
		model.addAttribute("email", email!=null?email:"");
		model.addAttribute("cellPhone", cellPhone!=null?cellPhone:"");
		model.addAttribute("firstName", firstName!=null?firstName:"");
		model.addAttribute("lastName", lastName!=null?lastName:"");
		model.addAttribute("service", "whydah");
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		
		if(!userTokenFound) {
			//register process, we show consents here
		}
		return "confirm_basicinfo";
	}

	private String toCredentialConfirm(Model model, String redirectURI, String provider, String username, String confirmError) {
		if(confirmError!=null && !confirmError.isEmpty()) {
			model.addAttribute("confirmError", confirmError);
		}
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("service", "whydah");
		model.addAttribute("whydahOauth2Provider", provider);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		return "confirm_credential";
	}

	private String confirmUserInfoCheckAndReturn(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, Model model, String redirectURI, String userticket, String userTokenXml, String provider) {
		
		updateLifeSpanForAuthSession(httpRequest, userTokenXml, provider);
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

	@RequestMapping("/whydah_basicinfo_confirm")
	public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		String provider = httpRequest.getParameter("whydahOauth2Provider");
		if(!whydahOauthHelper.getProviders().containsKey(provider) || !whydahOauthHelper.getProviders().get(provider).isEnabled()) {
			log.info("whydahlogin return login");
			return "login";
		}

		
		if (!whydahOauthHelper.isAuthenticated(httpRequest, provider)) {
			// not authenticated, redirecting to login.microsoft.com so user can authenticate
			String aad_auth_url = whydahOauthHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
//			model.addAttribute("redirect", aad_auth_url);
//			log.info("Redirecting to {}", aad_auth_url);
//			return "action";
			return toAction(model, aad_auth_url);
		}

		if (whydahOauthHelper.isAccessTokenExpired(httpRequest, provider)) {
			whydahOauthHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse, provider);
		}
		
		String newRegister = httpRequest.getParameter("newRegister");
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

		
		String stored_accessToken = WhydahOAuthSessionManagementHelper.getAccessToken(httpRequest, provider);
		String provider_uid = WhydahOAuthSessionManagementHelper.getUID(httpRequest, provider);

		if(userName==null || userName.isEmpty()) {
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
		
		
		WhydahOAuthUserCredential credential = new WhydahOAuthUserCredential(provider_uid, userName, provider_uid);
		String userticket = UUID.randomUUID().toString();
		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);

		if (userTokenXml == null) {	
			if(newRegister.equalsIgnoreCase("true")) {
				//save a new user with uid as username, and have personRef pointed to a new registered username 
				credential = new WhydahOAuthUserCredential(provider_uid, provider_uid, provider_uid);
				String personRef = userName;
				userTokenXml = tokenServiceClient.createAndLogonUser(provider, stored_accessToken, null, provider_uid, provider_uid, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);				
				if(userTokenXml!=null) {
					credential = new WhydahOAuthUserCredential(provider_uid, userName, provider_uid);
					userTokenXml = tokenServiceClient.createAndLogonUser(provider, stored_accessToken, null, provider_uid, userName, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);
				}
			} else {
				String personRef = UUID.randomUUID().toString();
				userTokenXml = tokenServiceClient.createAndLogonUser(provider, stored_accessToken, null, provider_uid, userName, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);
			}
		}


		if(userTokenXml!=null) {
			
			String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
			String uid = UserTokenXpathHelper.getUserID(userTokenXml);
			String username = UserTokenXpathHelper.getUserName(userTokenXml);
			
			setWhydahCookie(httpRequest, httpResponse, userTokenXml);
			updateLifeSpanForAuthSession(httpRequest, userTokenXml, provider);


			SessionDao.instance.saveRoleDatatoWhydah(uid, APPID, APPNAME, "Whydah", "PersonId", personRef);
			SessionDao.instance.syncWhydahUserInfoWithThirdPartyUserInfo(UserTokenXpathHelper.getUserID(userTokenXml), provider, stored_accessToken, resolveAppRoles(null), provider_uid, username, firstName, lastName, email, cellPhone, personRef);


			return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket));
		} else {
			return toLogin(model, redirectURI, "failure solving usertokenxml");
		}

	}

	private void updateLifeSpanForAuthSession(HttpServletRequest httpRequest, String userTokenXml, String provider) {
		Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
		Long tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
		Long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;

		//update the lifespance of the current session if existing in the map and cookie expirattion date
		WhydahOAuthSessionManagementHelper.updateLifeSpanForSession(httpRequest, endOfTokenLifeMs, provider);
	}

	private String resolveAppRoles(String approles) {
		if(approles==null) {
			approles="User";
		}
		return approles;
	}

	@RequestMapping("/whydah_credential_confirm")
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		String provider = httpRequest.getParameter("whydahOauth2Provider");
		
		if(!whydahOauthHelper.getProviders().containsKey(provider) || !whydahOauthHelper.getProviders().get(provider).isEnabled()) {
			log.info("whydahlogin return login");
			return "login";
		}


		if (!whydahOauthHelper.isAuthenticated(httpRequest, provider)) {
		
			String whydah_auth_url = whydahOauthHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
			return toAction(model, whydah_auth_url);
//			model.addAttribute("redirect", whydah_auth_url);
//			log.info("Redirecting to {}", whydah_auth_url);
//			return "action";
		}

		if (whydahOauthHelper.isAccessTokenExpired(httpRequest, provider)) {
			whydahOauthHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse, provider);
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
				return toCredentialConfirm(model, redirectURI, provider, userName, "Login failed. Wrong password!");	
			} else {
				return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket, userTokenXml, provider);
			}
		} else {
			if (SessionDao.instance.checkIfUserExists(userName)) {
				return toCredentialConfirm(model, redirectURI, provider, userName, "Username already existed!");
			} else {
				String firstName = WhydahOAuthSessionManagementHelper.getFirstName(httpRequest, provider);
				String lastName = WhydahOAuthSessionManagementHelper.getLastName(httpRequest, provider);
				String email = WhydahOAuthSessionManagementHelper.getEmail(httpRequest, provider); 
				String cellPhone = WhydahOAuthSessionManagementHelper.getCellPhone(httpRequest, provider);
				return toBasicInfoConfirm(model, redirectURI, provider, userName, firstName, lastName, email, cellPhone, false, true);
			}		
		}
	}
}
