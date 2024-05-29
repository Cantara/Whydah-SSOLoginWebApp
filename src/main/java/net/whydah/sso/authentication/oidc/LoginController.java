package net.whydah.sso.authentication.oidc;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import com.nimbusds.oauth2.sdk.GeneralException;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.WhydahProvider;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;

public class LoginController {

	private final static Logger log = LoggerFactory.getLogger(LoginController.class);

	private final WhydahServiceClient tokenServiceClient;
	private final String provider;
	private final boolean loginEnabled;
	private AuthHelper authHelper;
	private SessionManagementHelper sessionManagementHelper;
	private String jwtClaimAsUserName = "sub";

	public LoginController(String provider) throws GeneralException, IOException, URISyntaxException {
		this.provider = provider;
		Properties properties = AppConfig.readProperties();
		this.tokenServiceClient = SessionDao.instance.getServiceClient();
		//this.service = "oidcProvider" + provider.substring(0, 1).toUpperCase() + provider.substring(1);
		String enabled = properties.getProperty(provider + ".enabled");
		this.loginEnabled =  enabled!=null && enabled.equals("true");
		if (this.loginEnabled) {
			this.sessionManagementHelper = new SessionManagementHelper(provider);
			String issuerUrl = properties.getProperty(provider + ".issuerUrl");
			String clientId = properties.getProperty(provider + ".clientId");
			if(issuerUrl==null || issuerUrl.isEmpty()) {
				throw new RuntimeException("azuread.issuerUrl required in the app config");
			}
			if(clientId==null || clientId.isEmpty()) {
				throw new RuntimeException("azuread.clientId required in the app config");
			}
			String clientSecret = properties.getProperty(provider + ".clientSecret");
			if(properties.containsKey(provider + ".jwtClaimAsUserName")) {
				jwtClaimAsUserName = properties.getProperty(provider + ".jwtClaimAsUserName");
			}
			if(provider.equalsIgnoreCase("azuread")) {
				String tenantId = properties.getProperty(provider + ".tenantId");
				if(tenantId==null || tenantId.isEmpty()) {
					throw new RuntimeException("azuread.tenantId required in the app config");
				}
				issuerUrl +=  "f8cdef31-a31e-4b4a-93e4-5f571e91255a" + "/v2.0";
			}
			this.authHelper = new AuthHelper(provider, issuerUrl, clientId, clientSecret,
					new String[] { "openid", "name", "phoneNumber", "email", "profile" }, this.sessionManagementHelper);
			SessionDao.instance.addOIDCProvider(this.authHelper);
		} else {
			this.authHelper = null;
		}
		
	}
	
	public LoginController(WhydahProvider whydahProvider) throws GeneralException, IOException, URISyntaxException {
		this.tokenServiceClient = SessionDao.instance.getServiceClient();
		
		this.provider = whydahProvider.getProvider();
		
		this.loginEnabled =  whydahProvider.isEnabled();
		if (this.loginEnabled) {
			this.sessionManagementHelper = new SessionManagementHelper(provider);
			
			String issuerUrl = whydahProvider.getOauthUrl();
			String clientId = whydahProvider.getOauthClientId();
			String clientSecret = "NOTSET";
			if(issuerUrl==null || issuerUrl.isEmpty()) {
				throw new RuntimeException(provider + ".issuerUrl required in the app config");
			}
			if(clientId==null || clientId.isEmpty()) {
				throw new RuntimeException(provider + ".clientId required in the app config");
			}
			this.authHelper = new AuthHelper(provider, issuerUrl, clientId, clientSecret,
					new String[] { "openid", "name", "phoneNumber", "email", "profile" }, this.sessionManagementHelper);
			SessionDao.instance.addOIDCProvider(this.authHelper);
		} else {
			this.authHelper = null;
		}
	}


	public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model)
			throws Throwable {
		
		log.info("provider {} - login start", provider);
		
		String redirectURI = httpRequest.getParameter("redirectURI");
        
        String params ="";
        
        if(redirectURI!=null) {
        	params += (params.equals("")?"":"&") + "redirectURI=" + redirectURI;
        }
        
        String ssolwaReDirectURI = SessionDao.instance.LOGIN_URI + (params.equals("")?"":("?" + params));
        
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		if (!loginEnabled) {
			//to login page
			log.warn("provider {} is not enabled. Redirect to SSOLWA login page {}", provider, ssolwaReDirectURI);
			return toAction(model, ssolwaReDirectURI);
		}
		if (!authHelper.isAuthenticated(httpRequest)) {
			String auth_url = authHelper.getAuthRedirect(ssolwaReDirectURI);
			return toAction(model, auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				
				log.info("provider {} - Session expired, using silent flow", provider);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch (Exception ex) {
				ex.printStackTrace();
				String auth_url = authHelper.getAuthRedirect(ssolwaReDirectURI);
				log.warn("provider {} - Cannot use silent flow. Redirect to {}", provider, auth_url);
				return toAction(model, auth_url);
			}
		}
		// return redirectURi with a ticket
		log.info("provider {} - Seesion OK - Resolving login", provider);
		return resolve(httpRequest, httpResponse, model, ssolwaReDirectURI);
	}

	public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model)
			throws Throwable {
		log.info("provider {} - authentication process start", provider);
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		
		// get the original redirectURI
		String redirectURI = authHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(httpRequest);
		
		if (!loginEnabled) {
			log.warn("provider {} is not enabled. Redirect to SSOLWA login page {}", provider, redirectURI);
			return toAction(model, redirectURI);
		}
		
		// append the ticket and return to our client
		return resolve(httpRequest, httpResponse, model, redirectURI);
	}
	
	private String formatPhoneNumber(String phonenumber) {
		
		phonenumber = phonenumber.replace(" ", "");
		if(phonenumber.length()>=11 && phonenumber.startsWith("+47")) {
			phonenumber = phonenumber.replace("+47", "");
		}
		if(phonenumber.length()>=10 && phonenumber.startsWith("47")) {
			phonenumber = phonenumber.replace("47", "");
		}
		
		phonenumber = phonenumber.replace("+","");
		return phonenumber;
	}

	private String resolve(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model,
			String redirectURI) throws Exception {
		
		String firstName = sessionManagementHelper.getFirstName(httpRequest); // (String) payload.get("given_name");
		String lastName = sessionManagementHelper.getLastName(httpRequest); // (String) payload.get("family_name");
		String email = sessionManagementHelper.getEmail(httpRequest);
		String phoneNumber = sessionManagementHelper.getPhoneNumber(httpRequest);
		String subject = sessionManagementHelper.getSubject(httpRequest);
	
		
		phoneNumber = formatPhoneNumber(phoneNumber);
		
		String username = String.valueOf(sessionManagementHelper.getClaim(httpRequest, jwtClaimAsUserName));
		if(jwtClaimAsUserName.equalsIgnoreCase("phone_number")) {
			if(username!=null) {
				username = formatPhoneNumber(username);
			}
		}
		
		if(username==null) {
			//to ssolwa login page
			return toAction(model, redirectURI);
		}

		String userticket = UUID.randomUUID().toString();

		UserCredential credential = new UserCredential(username, subject);
		log.info("provider {} - Check user credential {}", provider, credential);

		// already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
		log.info("provider {} - UserTokenXml returned: {}", provider, userTokenXml);
		
		if (userTokenXml == null) {
			// register a cookie that contains sessionid which can be used on other server instances
			registerAuthCookie(httpRequest, httpResponse);

			if (!SessionDao.instance.checkIfUserExists(username)) {
				// we should ask user to confirm their info and give their consents as follows
				log.info("provider {} - Return to BasicInfoConfirm", provider);
				return toBasicInfoConfirm(model, redirectURI, username, firstName, lastName, email, phoneNumber, false);
			
			} else {
				log.info("provider {} - Found username {} exists", provider, username);
				//try if a session existing for this trusted client
				userTokenXml = tokenServiceClient.logOnBySharedSecrect(username, userticket);
				if(userTokenXml!=null) {
					return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket,
							userTokenXml);
				} else {
					
					return toCredentialConfirm(model, redirectURI, subject, username, null);
				}
				
			}

		} else {
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket,
					userTokenXml);
		}
	}
	

	// add this session to cookie, which can be used on other server instances
	/*
	 * 
	 * Imagine we fall into this scenario 1. SSOLWA-1 handles the redirect call and
	 * establishes a session (sessonid, sessiondata) and calls the "confirm" page 2.
	 * The POST "confirm" request is then handled in SSOLWA-2 - the one has no
	 * knowledge of the current session that SSOLWA-1 is processing
	 */
	private void registerAuthCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String sessionid = httpRequest.getSession().getId();
		SessionCookieHelper.addSessionCookie(provider, sessionid, httpResponse);
	}

	private String toBasicInfoConfirm(Model model, String redirectURI, String username, String firstName,
			String lastName, String email, String cellPhone, boolean userTokenFound) {
		
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("slackuser", "");
		model.addAttribute("email", email != null ? email : "");
		model.addAttribute("cellPhone", cellPhone != null ? cellPhone : "");
		model.addAttribute("firstName", firstName != null ? firstName : "");
		model.addAttribute("lastName", lastName != null ? lastName : "");
		model.addAttribute("provider", this.provider);
		model.addAttribute("jwtClaimAsUserName", this.jwtClaimAsUserName);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);

		if (!userTokenFound) {
			// register process, we show consents here
		}
		return "confirm_basicinfo";
	}

	private String toCredentialConfirm(Model model, String redirectURI, String clientId, String username, String loginErrorType) {
		
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("clientId", clientId);
		model.addAttribute("provider", this.provider);
		if (loginErrorType != null) {
			model.addAttribute("loginErrorType", loginErrorType);
		}
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "confirm_credential";
	}
	

	private String confirmUserInfoCheckAndReturn(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			Model model, String redirectURI, String userticket, String userTokenXml) {
		
		String r = appendTicketToRedirectURI(redirectURI, userticket);
		log.info("provider {} - Login resolved sucessfully. Redirect to {}", provider, r);
		updateLifeSpanForAuthSession(httpRequest, userTokenXml);
		setWhydahCookie(httpRequest, httpResponse, userTokenXml);	
		return toAction(model, r);
	}
	
	private void setWhydahCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			String userTokenXml) {
		String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
		Integer tokenRemainingLifetimeSeconds = WhydahServiceClient
				.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
		CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, httpRequest,
				httpResponse);
	}

	private String toAction(Model model, String redirectURI) {
		log.info("provider {} - Redirecting to {}", provider, redirectURI);
		model.addAttribute("redirect", redirectURI);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "action";
	}
	
	private String extractDomain(String username) {
		String domain = null;
		if (username.contains("@")) {
			domain = username.split("@")[1];
		}
		return domain;
	}

	public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model)
			throws Throwable {
		
		log.info("provider {} - ConfirmBasicInfo request", provider);
		
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		
		if (!authHelper.isAuthenticated(httpRequest)) {
			String auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
			log.warn("provider {} - Session Not found - Redirect {}", provider, auth_url);
			return toAction(model, auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.warn("provider {} - Session Expired. Use silent flow", provider);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch (Exception ex) {
				String auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
				log.warn("provider {} - Can not use silent flow. Return to {}", provider, auth_url);
				return toAction(model, auth_url);
			}
		}

		String userName = httpRequest.getParameter("username");
		String firstName = httpRequest.getParameter("firstName");
		String lastName = httpRequest.getParameter("lastName");
		String email = httpRequest.getParameter("email");
		String cellPhone = httpRequest.getParameter("cellPhone");
		
		//use same as 1881
		cellPhone = formatPhoneNumber(cellPhone);
		
//		if (cellPhone != null) {
//			cellPhone = cellPhone.replaceAll(" ", "").trim();
//			if (cellPhone.length() == 8) {
//				cellPhone = "+47" + cellPhone;
//			}
//		}
		
		String redirectURI = httpRequest.getParameter("redirectURI");
		String slackuser = httpRequest.getParameter("slackuser");

		if (slackuser == null || slackuser.isEmpty()) {
			slackuser = "unknown";
		}
		if (redirectURI == null || redirectURI.isEmpty()) {
			redirectURI = SessionDao.instance.LOGIN_URI;
		}
		
		if (!loginEnabled) {
			return toAction(model, redirectURI);
		}

		String stored_sub = sessionManagementHelper.getSubject(httpRequest);
		String stored_accessToken = sessionManagementHelper.getAccessToken(httpRequest);
		String stored_userinfo_jsson = sessionManagementHelper.getUserInfoJsonstring(httpRequest);
		String stored_username = String.valueOf(sessionManagementHelper.getClaim(httpRequest, jwtClaimAsUserName));
		
		if(jwtClaimAsUserName.equalsIgnoreCase("phone_number")) {
			if(stored_username!=null) {
				stored_username = formatPhoneNumber(stored_username);
			}
		}
		
		if (userName == null || (!userName.equalsIgnoreCase(stored_username))) {
			// misuse -to ssolwa login page
			return toAction(model, redirectURI);
		}		
		if (email == null || email.isEmpty()) {
			// misuse -to ssolwa login page
			return toAction(model, redirectURI);
		}
		if (cellPhone == null || cellPhone.isEmpty()) {
			// misuse -to ssolwa login page
			return toAction(model, redirectURI);
		}

		UserCredential credential = new UserCredential(userName, stored_sub);
		String userticket = UUID.randomUUID().toString();
		// already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);

		if (userTokenXml == null) {
			String personRef = UUID.randomUUID().toString();
			
			userTokenXml = tokenServiceClient.createAndLogonUser(provider, stored_accessToken, null, stored_userinfo_jsson, stored_sub, userName,
					firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);
		}

		if (userTokenXml != null) {

			String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
			String uid = UserTokenXpathHelper.getUserID(userTokenXml);
			String username = UserTokenXpathHelper.getUserName(userTokenXml);

			setWhydahCookie(httpRequest, httpResponse, userTokenXml);
			updateLifeSpanForAuthSession(httpRequest, userTokenXml);
			
			SessionDao.instance.saveRoleDatatoWhydah(uid, "PersonId", personRef);
			
			SessionDao.instance.syncWhydahUserInfoWithThirdPartyUserInfo(
					UserTokenXpathHelper.getUserID(userTokenXml),
					provider, 
					stored_accessToken, 
					resolveAppRoles(null), 
					stored_userinfo_jsson, 
					stored_sub,
					username, 
					firstName, 
					lastName, 
					email,
					cellPhone, 
					personRef);

	
		
			
			//String domain = extractDomain(email);
			
			return toAction(model, appendTicketToRedirectURI(redirectURI, userticket));
		} else {
			// to ssolwa login page
			return toAction(model, redirectURI);
		}

	}
	
	String appendTicketToRedirectURI(String redirectURI, String userticket) {
		String[] parts = redirectURI.split("\\?", 2);
		String r ="";
		if(parts.length==2) {
			r = parts[0] + "?userticket=" + userticket + "&" + parts[1]; 
		} else {
			r = parts[0] + "?userticket=" + userticket;
		}
		return r;
	}
	

	private void updateLifeSpanForAuthSession(HttpServletRequest httpRequest, String userTokenXml) {
		Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
		Long tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
		Long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;

		// update the lifespance of the current session if existing in the map and
		// cookie expirattion date
		sessionManagementHelper.updateLifeSpanForSession(httpRequest, endOfTokenLifeMs);
	}

	private String resolveAppRoles(String approles) {

		if (approles == null) {
			approles = "User";
		}
		return approles;
	}
	
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model)
			throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		

		if (!authHelper.isAuthenticated(httpRequest)) {
			String auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
			log.warn("provider {} - Session Not found - Redirect {}", provider, auth_url);
			return toAction(model, auth_url);
		}

		if (authHelper.isAccessTokenExpired(httpRequest)) {
			try {
				log.warn("provider {} - Session Expired. Use silent flow", provider);
				authHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			} catch (Exception ex) {
				String auth_url = authHelper.getAuthRedirect(httpRequest.getParameter("redirectURI"));
				log.warn("provider {} - Can not use silent flow. Return to {}", provider, auth_url);
				return toAction(model, auth_url);
			}
		}
		
		

		String clientId = httpRequest.getParameter("clientId");
		String userName = httpRequest.getParameter("username");
		String pwd = httpRequest.getParameter("password");
		String redirectURI = httpRequest.getParameter("redirectURI");
		String userTokenXml = null;
		String userticket = UUID.randomUUID().toString();
		
		if (!this.loginEnabled) {
			log.warn("provider {} is not enabled. Redirect to SSOLWA login page {}", provider, redirectURI);
			return toAction(model, redirectURI);
		}
		
		if (userName != null && pwd != null && userName.length() > 0 && pwd.length() > 0) {
			userTokenXml = tokenServiceClient.getUserToken(new UserCredential(userName, pwd), userticket);
		}

		if (userTokenXml == null) {
			return toCredentialConfirm(model, redirectURI, clientId, userName, "password");
		} else {
			return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket,
					userTokenXml);
		}
	}
	
}

