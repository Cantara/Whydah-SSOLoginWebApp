package net.whydah.sso.authentication.iamproviders.azuread;

import com.jayway.jsonpath.JsonPath;
import com.microsoft.aad.msal4j.IAccount;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.authentication.iamproviders.google.GoogleSessionManagementHelper;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.user.Email;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Controller
public class AzureSSOLoginController {

	private final static Logger log = LoggerFactory.getLogger(AzureSSOLoginController.class);
	protected String LOGOURL = "/sso/images/site-logo.png";
	@Autowired
	AzureADAuthHelper aadHelper;
	private String APPID="", APPNAME="";
	private final WhydahServiceClient tokenServiceClient;
	
	public AzureSSOLoginController() throws IOException {
		Properties properties = AppConfig.readProperties();
		tokenServiceClient = SessionDao.instance.getServiceClient();
		LOGOURL = properties.getProperty("logourl");
		APPID = properties.getProperty("applicationid");
		APPNAME = properties.getProperty("applicationname");
	}
	
	@RequestMapping("/aadprelogin")
	public String prelogin(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
		
		String redirectURI = httpRequest.getParameter("redirectURI");
		model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
			return "login";
		} else {
			return "login_with_microsoft";
		}
	}


	@RequestMapping("/aadlogin")
	public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
		
		
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
			return "login";
		}
		
		
		if (!aadHelper.isAuthenticated(httpRequest) || aadHelper.isAccessTokenExpired(httpRequest)) {
			String redirectURI = httpRequest.getParameter("redirectURI");
			// not authenticated, redirecting to login.microsoft.com so user can authenticate
			String aad_auth_url = aadHelper.getAuthRedirect(httpRequest, httpRequest.getParameter(ConstantValue.LOGIN_HINT), redirectURI);
			model.addAttribute("redirect", aad_auth_url);
			log.info("Redirecting to {}", aad_auth_url);
			return "action";
		}

		//TODO: check the silent flow later. It is a bug here from the microsoft client lib 1.15.0 . We may update the dependency
		//if (aadHelper.isAccessTokenExpired(httpRequest)) {
			//aadHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
		//}
		
		//return redirectURi with a ticket
		return resolve(httpRequest, httpResponse, model, httpRequest.getParameter("redirectURI"));
	}

	@RequestMapping("/aadauth")
	public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
			return "login";
		}
		//get the original redirectURI
		StateData stateData = aadHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(httpRequest);
		String redirectURI = stateData.getRedirectURI();
		//append the ticket and return to our client
		return resolve(httpRequest, httpResponse, model, redirectURI);
	}

	@RequestMapping("/aad_credential_confirm")
	public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		String realEstate = httpRequest.getParameter("real_estate"); 
		
		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
			return "login";
		}
		try {

			if (!aadHelper.isAuthenticated(httpRequest)) {
				// not authenticated, redirecting to login.microsoft.com so user can authenticate

				String aad_auth_url = aadHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
				model.addAttribute("redirect", aad_auth_url);
				log.info("confirmExist - Redirecting to {}", aad_auth_url);
				return "action";
			}

			if (aadHelper.isAccessTokenExpired(httpRequest)) {
				aadHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
			}
			
			String newRegister = httpRequest.getParameter("newRegister");
			String userName = httpRequest.getParameter("username");
			String pwd = httpRequest.getParameter("password");
			String redirectURI = httpRequest.getParameter("redirectURI");
			String userTokenXml = null;
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

				//info from 3rd party
				//IAuthenticationResult auth = AzureSessionManagementHelper.getAuthSessionObject(httpRequest);
				String accessToken = AzureSessionManagementHelper.getAccessToken(httpRequest);
				String idToken = AzureSessionManagementHelper.getIdToken(httpRequest);
				JWTClaimsSet claims = JWTParser.parse(idToken).getJWTClaimsSet();
				String tenantId = claims.getStringClaim("tid"); //tenant id
				String oid = claims.getStringClaim("oid"); //principal id

				IAccount account = AzureSessionManagementHelper.getAccount(httpRequest);
				if (!userName.equalsIgnoreCase(account.username())) {
					//misuse
					return toLogin(model, redirectURI, "misuse of username");
				}


				String userInfo = aadHelper.getUserInfoFromGraph(accessToken);  //userinfo
				String approles = aadHelper.getAppRoleAssignments(accessToken); //app roles of this user
				log.info("confirmExist resolving user info tenantId {} - account username {}, home_accountid {}, environemnt {}, userinfo {}, roles {}", tenantId, account.username(), account.homeAccountId(), account.environment(), userInfo, approles);

				String firstName = claims.getStringClaim("given_name");
				String lastName = claims.getStringClaim("family_name");
				String email = null, cellPhone = null;
				if (userInfo != null) {
					email = JsonPath.read(userInfo, "$.mail");
					cellPhone = JsonPath.read(userInfo, "$.mobilePhone");
					if (cellPhone != null) {
						cellPhone = cellPhone.replaceAll(" ", "").trim();
						if (cellPhone.length() == 8) {
							cellPhone = "+47" + cellPhone;
						}
					}
				}

				if (email == null) {
					if (Email.isValid(account.username())) {
						email = account.username();
					} else {
						return toLogin(model, redirectURI, "failure solving email");
					}
				}

				return toBasicInfoConfirm(model, redirectURI, "aad-" + userName, firstName, lastName, email, null, false, true);


			

			}
		} catch (Exception ee) {
			log.error("confirmExist exception", ee);
			throw ee;
		}
		
	}

	@RequestMapping("/aad_basicinfo_confirm")
	public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws
	Throwable {
		String userTokenXml = null;
		String redirectURI = null;
		String realEstate = httpRequest.getParameter("real_estate"); 
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_LoginTypes(model);
		if(realEstate!=null && realEstate.equalsIgnoreCase("SOP5")) {
			model.addAttribute("realEstate", realEstate);
			model.addAttribute("realEstateBanner", "/sso/images/common/SOP5banner.png");
		}

		if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
			return "login";
		}
		
		try {
			
			if (!aadHelper.isAuthenticated(httpRequest)) {
				// not authenticated, redirecting to login.microsoft.com so user can authenticate
				
				String aad_auth_url = aadHelper.getAuthRedirect(httpRequest, httpRequest.getParameter("username"), httpRequest.getParameter("redirectURI"));
				model.addAttribute("redirect", aad_auth_url);
				log.info("confirmBasicInfo - Redirecting to {}", aad_auth_url);
				return "action";
			}

			if (aadHelper.isAccessTokenExpired(httpRequest)) {
				aadHelper.updateAuthDataUsingSilentFlow(httpRequest, httpResponse);
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
			redirectURI = httpRequest.getParameter("redirectURI");
			String slackuser = httpRequest.getParameter("slackuser");

			if (slackuser == null || slackuser.isEmpty()) {
				slackuser = "unknown";
			}
			if (redirectURI == null || redirectURI.isEmpty()) {
				redirectURI = "welcome";
			}

			//IAuthenticationResult auth = AzureSessionManagementHelper.getAuthSessionObject(httpRequest);
			
			String accessToken = AzureSessionManagementHelper.getAccessToken(httpRequest);
			String idToken = AzureSessionManagementHelper.getIdToken(httpRequest);
			JWTClaimsSet claims = JWTParser.parse(idToken).getJWTClaimsSet();
			String oid = claims.getStringClaim("oid"); //principal id

			IAccount account = AzureSessionManagementHelper.getAccount(httpRequest);
			String approles = aadHelper.getAppRoleAssignments(accessToken); //app roles of this user

			if (userName == null || (!userName.equalsIgnoreCase(account.username()) && !userName.equalsIgnoreCase("aad-" + account.username()))) {
				return toLogin(model, redirectURI, "illegal username");
			}
			if (firstName == null || firstName.isEmpty()) {
				return toLogin(model, redirectURI, "illegal first name");
			}
			if (lastName == null || lastName.isEmpty()) {
				return toLogin(model, redirectURI, "illegal last name");
			}
			if (email == null || email.isEmpty() || !Email.isValid(email)) {
				return toLogin(model, redirectURI, "illegal email");
			}
			if (cellPhone == null || cellPhone.isEmpty()) {
				return toLogin(model, redirectURI, "illegal cell phone");
			}
			String domain = AzureSessionManagementHelper.getSessionSelectedDomain(httpRequest);
			if(domain == null) {
				domain = extractDomain(email);
			}

			AzureADUserCredential credential = new AzureADUserCredential(oid, userName, oid);
			String userticket = UUID.randomUUID().toString();
			//already found check
			userTokenXml = tokenServiceClient.getUserToken(credential, userticket);

			if (userTokenXml == null) {
				String personRef = UUID.randomUUID().toString();
				userTokenXml = tokenServiceClient.createAndLogonUser("aad", accessToken, approles, "", oid, userName, firstName, lastName, email, cellPhone, personRef, credential, userticket, httpRequest);
			}

			if (userTokenXml != null) {
				
				updateLifeSpanForAuthSession(httpRequest, userTokenXml);
				setWhydahCookie(httpRequest, httpResponse, userTokenXml);
				String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
				String uid = UserTokenXpathHelper.getUserID(userTokenXml);
				String username = UserTokenXpathHelper.getUserName(userTokenXml);

				
				try {
					SessionDao.instance.saveRoleDatatoWhydah(uid, "PersonId", personRef);			
					SessionDao.instance.syncWhydahUserInfoWithThirdPartyUserInfo(UserTokenXpathHelper.getUserID(userTokenXml), "aad", accessToken, resolveAppRoles(approles), "", oid, username, firstName, lastName, email, cellPhone, personRef);
				} catch (Exception e) {
					log.error("Unable to persist roles to whydah");
				}

				return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket) + "&referer_channel=" + domain );
			} else {

				return toLogin(model, redirectURI, "failure solving usertokenxml");
			}

		} catch (Exception e) {
			log.error("AzureAD auth gknordic.com exception", e);
		}

		return toLogin(model, redirectURI, "failure solving usertokenxml");


	}
	
	private void updateLifeSpanForAuthSession(HttpServletRequest httpRequest, String userTokenXml) {
		Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
		Long tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
		Long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;

		//update the lifespance of the current session if existing in the map and cookie expirattion date
		AzureSessionManagementHelper.updateLifeSpanForSession(httpRequest, endOfTokenLifeMs);
	}

	private String resolve(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model, String
			redirectURI) throws Exception {
		if (redirectURI == null || redirectURI.contentEquals("")) {
			redirectURI = "welcome";
		}
		
		//info from 3rd party
		//IAuthenticationResult auth = AzureSessionManagementHelper.getAuthSessionObject(httpRequest);
		String accessToken = AzureSessionManagementHelper.getAccessToken(httpRequest);
		String idToken = AzureSessionManagementHelper.getIdToken(httpRequest);
		String userInfo = aadHelper.getUserInfoFromGraph(accessToken);  //userinfo
		log.debug("access token returned {}", accessToken);
		log.debug("id token returned {}", idToken);
		
		JWTClaimsSet claims = JWTParser.parse(idToken).getJWTClaimsSet();
		String tenantId = claims.getStringClaim("tid"); //tenant id
		String oid = claims.getStringClaim("oid"); //principal id

		IAccount account = AzureSessionManagementHelper.getAccount(httpRequest);
		//String userInfo = aadHelper.getUserInfoFromGraph(accessToken);  //userinfo
		String approles = aadHelper.getAppRoleAssignments(accessToken); //app roles of this user
		log.info("resolve resolving user info tenantId {} - account username {}, home_accountid {}, environemnt {}, userinfo {}, roles {}", tenantId, account.username(), account.homeAccountId(), account.environment(), userInfo, approles);

		//String firstName = JsonPath.read(userInfo, "$.givenName");
		//String lastName = JsonPath.read(userInfo, "$.surname");
		String adusername = account.username();

		String firstName = claims.getStringClaim("given_name");
		String lastName = claims.getStringClaim("family_name");
		String email = claims.getStringClaim("email");
		String cellPhone = null;
		if (userInfo != null) {
			cellPhone = JsonPath.read(userInfo, "$.mobilePhone");
			if (cellPhone != null) {
				cellPhone = cellPhone.replaceAll(" ", "").trim();
				if (cellPhone.length() == 8) {
					cellPhone = "+47" + cellPhone;
				}
			}
		}

		String userticket = UUID.randomUUID().toString();

		if (email == null) {
			if (Email.isValid(account.username())) {
				email = account.username();
			} else {
				log.warn("resolve Illegal usermame:" + account.username());
			}
		}

		AzureADUserCredential credential = new AzureADUserCredential(oid, adusername, oid);

		//already found check
		String userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
		log.info("resolve userTokenXml(1):" + userTokenXml);

		if (userTokenXml == null) {
			adusername = "aad-" + adusername;
			credential = new AzureADUserCredential(oid, adusername, oid);
			log.info("resolve credential(2):" + credential);
			userTokenXml = tokenServiceClient.getUserToken(credential, userticket);
			log.info("resolve userTokenXml(2):" + userTokenXml);

		}

		if (userTokenXml == null) {

			//register a cookie that contains sessionid which can be used on other server instances
			registerAuthCookie(httpRequest, httpResponse);


			if (!SessionDao.instance.checkIfUserExists(account.username())) {
				log.info("resolve -checkIfUserNameExists-false for:" + account.username());
				//we should ask user to confirm their info and give their consents as follows
				return toBasicInfoConfirm(model, redirectURI, account.username(), firstName, lastName, email, cellPhone, false, false);
			} else {
				log.info("resolve -checkIfUserNameExists-true for:" + account.username());
				//prompt "username exists. Is it you? Yes/No
				//if Yes, enter username/password. Otherwise, append the 3rd party marker to the username
				//                return toBasicInfoConfirm(model, redirectURI, adusername, firstName, lastName, email, cellPhone, false);
				//return toCredentialConfirm(model, redirectURI, account.username(), null);
			
			
			
				
				//try if a session existing for this trusted client
				userTokenXml = tokenServiceClient.logOnBySharedSecrect(account.username(), userticket);
				if(userTokenXml!=null) {
					return confirmUserInfoCheckAndReturn(httpRequest, httpResponse, model, redirectURI, userticket,
							userTokenXml);
				} else {
					
					return toCredentialConfirm(model, redirectURI, account.username(), null);
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
  		1. SSOLWA-1 handles the aad redirect call and establishes a session (sessonid, sessiondata) and calls the "confirm" page
  		2. The POST "confirm" request is then handled in SSOLWA-2 - the one has no knowledge of the current session that SSOLWA-1 is processing
	 */
	private void registerAuthCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String sessionid = httpRequest.getSession().getId();
		SessionCookieHelper.addSessionCookie("aad", sessionid, httpResponse);
	}

	private String confirmUserInfoCheckAndReturn(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, Model model, String redirectURI, String userticket, String
			userTokenXml) {
		
		updateLifeSpanForAuthSession(httpRequest, userTokenXml);
		setWhydahCookie(httpRequest, httpResponse, userTokenXml);
		
		String email = UserTokenXpathHelper.getEmail(userTokenXml);
		String domain = extractDomain(email);
		return toAction(model, tokenServiceClient.appendTicketToRedirectURI(redirectURI, userticket) + "&referer_channel=" + domain);
	}

	private String toLogin(Model model, String redirectURI, String error) {
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("loginError", "Login error: Could not create or authenticate user due to " + error);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "login";
	}

	private String toCredentialConfirm(Model model, String redirectURI, String username, String confirmError) {
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("service", "azuread");
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		if(confirmError!=null && !confirmError.isEmpty()) {
			model.addAttribute("confirmError", confirmError);
		}
		return "confirm_credential_azuread";
	}

	private String toBasicInfoConfirm(Model model, String redirectURI, String username, String firstName, String
			lastName, String email, String cellPhone, boolean userTokenFound, boolean newRegister) {
		model.addAttribute("newRegister", String.valueOf(newRegister));
		model.addAttribute("redirectURI", redirectURI);
		model.addAttribute("username", username);
		model.addAttribute("slackuser", "");
		model.addAttribute("email", email != null ? email : "");
		model.addAttribute("cellPhone", cellPhone != null ? cellPhone : "");
		model.addAttribute("firstName", firstName != null ? firstName : "");
		model.addAttribute("lastName", lastName != null ? lastName : "");
		model.addAttribute("service", "azuread");
		
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		

		//register process, must shows templates
		if (!userTokenFound) {
			
		}
		return "confirm_basicinfo_azuread";
	}

	private String extractDomain(String username) {
		String domain = null;
		if (username.contains("@")) {
			domain = username.split("@")[1];
		}
		return domain;
	}

	private void setWhydahCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String
			userTokenXml) {
		String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
		Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
		CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, httpRequest, httpResponse);
	}

	private String toAction(Model model, String redirectURI) {
		log.info("Redirecting to {}", redirectURI);
		model.addAttribute("redirect", redirectURI);
		SessionDao.instance.addModel_CSRFtoken(model);
		return "action";
	}

	private String resolveAppRoles(String approles) {
		//sample
		//{"@odata.context":"https://graph.microsoft.com/beta/$metadata#appRoleAssignments","value":[{"id":"tkDibD99skGDPepdQecQJFpgR6iUEuNCu9oc20UFA60","creationTimestamp":"2020-10-27T06:14:19.1940645Z","appRoleId":"47fbb575-859a-4941-89c9-0f7a6c30beac","principalDisplayName":"huy do","principalId":"6ce240b6-7d3f-41b2-833d-ea5d41e71024","principalType":"User","resourceDisplayName":"EntraSSO","resourceId":"7097bad7-d2a9-4e96-b362-cd2c570f5b48"}]}
		//TODO: we parse and extract the roles we need
		//for example, we can predefine different roles for EntraOS in Azure AD console
		//with "appRoleId":"47fbb575-859a-4941-89c9-0f7a6c30beac", we might have "admin" role
		//if the user has above appRoleId, he/she has "admin" role

		//for now just grab everything to the 3rd party userxml data
		if (approles == null) {
			approles = "User";
		}
		return approles;
	}
}
