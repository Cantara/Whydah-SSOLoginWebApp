package net.whydah.sso.dao;

import net.whydah.sso.ServerRunner;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.iamproviders.whydah.WhydahOAuthHelper;
import net.whydah.sso.authentication.iamproviders.whydah.WhydahOauthIntegrationConfig;
import net.whydah.sso.authentication.oidc.Provider;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.sso.commands.adminapi.user.CommandGetUserAggregate;
import net.whydah.sso.commands.adminapi.user.CommandUpdateUser;
import net.whydah.sso.commands.adminapi.user.CommandUpdateUserAggregate;
import net.whydah.sso.commands.adminapi.user.CommandUserExists;
import net.whydah.sso.commands.adminapi.user.CommandUserNameExists;
import net.whydah.sso.commands.adminapi.user.CommandUserPasswordLoginEnabled;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandDeleteUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandGetUserRoles;
import net.whydah.sso.commands.adminapi.user.role.CommandUpdateUserRole;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.LoginTypes;
import net.whydah.sso.ddd.model.application.RedirectURI;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.mappers.UserRoleMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserIdentity;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.utils.HazelcastMapHelper;
import net.whydah.sso.utils.ServerUtil;
import net.whydah.sso.utils.SignupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.map.IMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.text.Normalizer;
import java.util.*;

public enum SessionDao {

	instance;

	public final String DEFAULT_REDIRECT = "welcome";
	protected final static Logger log = LoggerFactory.getLogger(SessionDao.class);
	private WhydahServiceClient serviceClient=null;
	protected String LOGOURL = "/sso/images/site-logo.png";
	protected String whydahVersion = ServerRunner.version;
	protected URI uasServiceUri;
	protected URI tokenServiceUri;
	protected URI crmServiceUri;
	protected URI reportservice;
	protected Properties properties;
	UserCredential adminUserCredential;
	public String MY_APP_URI;
	public String LOGOUT_ACTION_URI;
	public String LOGIN_URI;
    private boolean matchRedirectURLtoModel = false;

    CRMHelper crmHelper;
	ReportServiceHelper reportServiceHelper;
	LoginTypes enabledLoginTypes;
	PersonaServiceHelper personaService;
	
	WhydahOauthIntegrationConfig whydahOauthConfig;
	
	public CRMHelper getCRMHelper(){
		return crmHelper;
	}

	public ReportServiceHelper getReportServiceHelper(){
		return reportServiceHelper;
	}
	
	private IMap<String, String> csrftokens = HazelcastMapHelper.register("csrftokens_map");
	private IMap<String, String> username_redirectURI = HazelcastMapHelper.register("username_redirectURI");

	private SessionDao() {

		try {

			properties = AppConfig.readProperties();
			this.LOGOURL = properties.getProperty("logourl", LOGOURL);
			MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
            LOGOUT_ACTION_URI = MY_APP_URI + "logoutaction";
            LOGIN_URI = MY_APP_URI + "login";
            enabledLoginTypes = new LoginTypes(properties);
            
			this.serviceClient = new WhydahServiceClient(properties);
			this.tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
			this.uasServiceUri = UriBuilder.fromUri(properties.getProperty("useradminservice")).build();
			this.crmServiceUri = UriBuilder.fromUri(properties.getProperty("crmservice")).build();
			this.reportservice = UriBuilder.fromUri(properties.getProperty("reportservice")).build();
			this.adminUserCredential = new UserCredential(properties.getProperty("uasuser"), properties.getProperty("uaspw"));
			this.crmHelper = new CRMHelper(serviceClient, crmServiceUri, properties.getProperty("email.verification.link"));
			this.reportServiceHelper = new ReportServiceHelper(serviceClient, reportservice);
			this.personaService = new PersonaServiceHelper(properties);
            this.matchRedirectURLtoModel = Boolean.getBoolean(properties.getProperty("matchRedirects"));
            whydahOauthConfig = new WhydahOauthIntegrationConfig(properties);
        } catch (IOException e) {

			e.printStackTrace();
		}

	}



	ObjectMapper objectMapper = new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
			.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
			.findAndRegisterModules();

	//////ADD BASIC VALUE TO THE MODEL

	public Model addModel_LoginTypes(Model model){
		
		model.addAttribute(ConstantValue.SIGNUPENABLED, enabledLoginTypes.isSignupEnabled());
		model.addAttribute(ConstantValue.USERPASSWORDLOGINENABLED, enabledLoginTypes.isUserpasswordLoginEnabled());
        model.addAttribute(ConstantValue.FACEBOOKLOGIN_ENABLED, enabledLoginTypes.isFacebookLoginEnabled());
        model.addAttribute(ConstantValue.OPENIDLOGIN_ENABLED, enabledLoginTypes.isOpenIdLoginEnabled());
        model.addAttribute(ConstantValue.OMNILOGIN_ENABLED, enabledLoginTypes.isOmniLoginEnabled());
        model.addAttribute(ConstantValue.NETIQLOGIN_ENABLED, enabledLoginTypes.isNetIQLoginEnabled());
        model.addAttribute(ConstantValue.PERSONASSHOTCUT_ENABLED, enabledLoginTypes.isPersonasShortcutEnabled());
        model.addAttribute(ConstantValue.GOOGLELOGIN_ENABLED, enabledLoginTypes.isGoogleLoginEnabled());
        model.addAttribute(ConstantValue.MICROSOFTLOGIN_ENABLED, enabledLoginTypes.isMicrosoftLoginEnabled());
        
        model.addAttribute(ConstantValue.WHYDAH_LOGININTEGRATION_PROVIDERS, whydahOauthConfig.getProviderMap().values());
        
        //sign up
        model.addAttribute(ConstantValue.SIGNUP_MICROSOFT_ON, enabledLoginTypes.isSignuppageMicrosoftOn());
        model.addAttribute(ConstantValue.SIGNUP_GOOGLE_ON, enabledLoginTypes.isSignuppageGoogleOn());
        model.addAttribute(ConstantValue.SIGNUP_FACEBOOK_ON, enabledLoginTypes.isSignuppageFacebookOn());
        model.addAttribute(ConstantValue.SIGNUP_NETIQ_ON, enabledLoginTypes.isSignuppageNetIQOn());
        model.addAttribute(ConstantValue.SIGNUP_WHYDAH_INTEGRATION_PROVIDERS_ON, enabledLoginTypes.isSignuppageWhydahIntegrationproviderOn());
        
       
       
        if (enabledLoginTypes.isNetIQLoginEnabled()) {
            setNetIQOverrides(model);
        }
        
        if(enabledLoginTypes.isPersonasShortcutEnabled()) {
        	model.addAttribute("personas", personaService.getPersonasCredentials());
        }

		for (Provider oidcProvider : enabledLoginTypes.getOIDCProvidersEnabled()) {
			model.addAttribute("oidcProvider"+oidcProvider.provider().substring(0, 1).toUpperCase() + oidcProvider.provider().substring(1)+"Enabled", true);
		}
        
        
		return model;
	}
	
	private void setNetIQOverrides(Model model) {
		try {
			model.addAttribute("netIQtext", AppConfig.readProperties().getProperty("logintype.netiq.text"));
			model.addAttribute("netIQimage", AppConfig.readProperties().getProperty("logintype.netiq.logo"));
		} catch (IOException ioe) {
			model.addAttribute("netIQtext", "NetIQ");
			model.addAttribute("netIQimage", "images/netiqlogo.png");
		}
	}
	
	public  boolean isLoginTypeEnabled(String loginType) {
		LoginTypes enabledLoginTypes = new LoginTypes(properties);
        if (loginType.equalsIgnoreCase(ConstantValue.FACEBOOKLOGIN_ENABLED)) {
            return enabledLoginTypes.isFacebookLoginEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.OPENIDLOGIN_ENABLED)) {
            return enabledLoginTypes.isOpenIdLoginEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.OMNILOGIN_ENABLED)) {
            return enabledLoginTypes.isOmniLoginEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.NETIQLOGIN_ENABLED)) {
            return enabledLoginTypes.isNetIQLoginEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.SIGNUPENABLED)) {
            return enabledLoginTypes.isSignupEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.GOOGLELOGIN_ENABLED)) {
            return enabledLoginTypes.isGoogleLoginEnabled();
        }
        if (loginType.equalsIgnoreCase(ConstantValue.MICROSOFTLOGIN_ENABLED)) {
            return enabledLoginTypes.isMicrosoftLoginEnabled();
        }
        
        return false;
    }

	public Model addModel_LOGO_URL(Model model){
		model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
		return model;
	}
	public Model addModel_MYURI(Model model){
		model.addAttribute(ConstantValue.MYURI, properties.getProperty("myuri"));
		return model;
	}
	public Model addModel_WHYDAH_VERSION(Model model){
		model.addAttribute(ConstantValue.WHYDAH_VERSION, whydahVersion);
		return model;
	}
	public Model addModel_CSRFtoken(Model model){
		model.addAttribute(ConstantValue.CSRFtoken, getCSRFtoken());
		return model;
	}
	public Model addModel_IAM_MODE(Model model){
		model.addAttribute(ConstantValue.IAM_MODE, ApplicationMode.getApplicationMode());
		return model;
	}
	public Model addModel_CONTEXTPATH(Model model){
		model.addAttribute(ConstantValue.MYURI, properties.getProperty("myuri"));
		return model;
	}
	
	//////END ADD BASIC VALUE TO THE MODEL


	
	
	//////HANDLE PARAMTERS FROM THE REQUEST

	public String getFromRequest_User(HttpServletRequest request){
		return request.getParameter(ConstantValue.USER);
	}
	
	public String getFromRequest_Password(HttpServletRequest request){
		return request.getParameter(ConstantValue.PASSWORD);
	}
	
	public String getfromRequest_CSRFtoken(HttpServletRequest request) {
		return request.getParameter(ConstantValue.CSRFtoken);//to check for valid request
	}
	
	public String getfromRequest_Address(HttpServletRequest request) {
		return request.getParameter(ConstantValue.ADDRESS);
	}
	
	public boolean getFromRequest_SessionCheck(HttpServletRequest request) {
		String redirectURI = request.getParameter(ConstantValue.SESSIONCHECK);
		if (redirectURI == null || redirectURI.length() < 1) {
			log.trace("isSessionCheckOnly - No SESSIONCHECK param found");
			return false;
		}
		return true;
	}

    public String getFromRequest_RedirectURI_Old(HttpServletRequest request) {
        String redirectURI = request.getParameter(ConstantValue.REDIRECT_URI);
		if (redirectURI == null || redirectURI.length() < 1) {
			log.trace("getRedirectURI - No redirectURI found, setting to {}", DEFAULT_REDIRECT);
			return DEFAULT_REDIRECT;
		}
		try {
			// TODO  Implement RedirectURI verification/swap here (from AppLinks)
			URI redirect = new URI(redirectURI);
			return redirectURI;
		} catch (Exception e) {
			return DEFAULT_REDIRECT;
		}
	}

    public String getFromRequest_RedirectURI(HttpServletRequest request) {
        try {
            String redirectURI = request.getParameter(ConstantValue.REDIRECT_URI);
            String hashContent = request.getParameter("hashContent");
            if (hashContent == null) {
                hashContent = "";
            } else {
                int origSize = hashContent.length();
                String withoutAccent = Normalizer.normalize(hashContent, Normalizer.Form.NFD);
                hashContent = withoutAccent.replaceAll("[^a-zA-Z0-9 ]", "");            //hashContent = sanitize(escapeHtml(hashContent.replace("alert", "").replace("confirm", "")));
                // If there are issues in the hashContent we return default
                if (origSize != hashContent.length()) {
                    return DEFAULT_REDIRECT;
                }
            }
            if (redirectURI == null || !(RedirectURI.isValid(redirectURI)) || redirectURI.equalsIgnoreCase("null") || redirectURI.equalsIgnoreCase("")) {
                log.trace("getRedirectURI - No redirectURI found, setting to {}", DEFAULT_REDIRECT);
                return DEFAULT_REDIRECT;
            }
            try {
                if (matchRedirectURLtoModel) {
                    redirectURI = new RedirectURI(redirectURI, getServiceClient().getWAS().getApplicationList(), null).getInput();
                } else {
                    redirectURI = new RedirectURI(redirectURI, null, null).getInput();
                }
                URI redirect = new URI(redirectURI + hashContent);
                return redirect.toString();
            } catch (Exception e) {
                return DEFAULT_REDIRECT;
            }
        } catch (Exception e) {
            return DEFAULT_REDIRECT;

        }
    }


	public String getFromRequest_CellPhone(HttpServletRequest request) {
		String cellPhone = request.getParameter(ConstantValue.CELL_PHONE);
		if (cellPhone == null || cellPhone.length() < 1) {
			log.trace("getCellPhone - No cellphone found, setting to ", "");
			return "";
		}
		return cellPhone;
	}
	
	public String getFromRequest_PhoneNo(HttpServletRequest request) {
		String phoneNo = request.getParameter("phoneNo");
		return phoneNo;
	}
	

	public String getFromRequest_UserTicket(HttpServletRequest request) {
		return request.getParameter(ConstantValue.USERTICKET);
	}

	public String getFromRequest_UserName(HttpServletRequest request){
		return request.getParameter(ConstantValue.USERNAME);
	}
	
	public String getFromRequest_Email(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.EMAIL), "");
	}
	
	public String getFromRequest_FirstName(HttpServletRequest request){
		 return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.FIRSTNAME), "");
	}
	
	public String getFromRequest_LastName(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.LASTNAME), "");
	}
	
	public String getFromRequest_Pin(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.PIN), "");
	}
	
	public String getFromRequest_StreetAddress(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.STREETADDRESS), "");
	}
	
	public String getFromRequest_ZipCode(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.ZIPCODE), "");
	}
	
	public String getFromRequest_City(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.CITY), "");
	}
	
	public String getFromRequest_OIDADDRESS(HttpServletRequest request){
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.OIDADDRESS),"");
	}
	
	public String getFRomRequest_RoleId(HttpServletRequest request) {
		return SignupHelper.getValueOrDefault(request.getParameter(ConstantValue.ROLE_ID),"");
	}
	
	public String getFromRequest_Token(HttpServletRequest request) {
		return request.getParameter(ConstantValue.TOKEN);
	}

	public String getFromJsonRequest_Email(HttpServletRequest request) {
		StringBuffer postedJsonString = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				postedJsonString.append(line);
		} catch (Exception e) { /*report an error*/ }

		log.trace("getFromJsonRequest_Email - postedJsonString: {}", postedJsonString);
		String email = JsonPathHelper.findJsonPathValue(postedJsonString.toString(), "$.email");
		return email;
	}
	
	//////END HANDLE PARAMTERS FROM THE REQUEST


    ///  Block side-channel attacks - looks correct bud does not clear the detectify "alert"
    public void setCSP(HttpServletResponse response) {
		response.addHeader("Content-Security-Policy", "frame-ancestors 'none'");
		response.addHeader("Content-Security-Policy-Report-Only", "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' https://fonts.googleapis.com 'unsafe-inline' data:; img-src 'self' data: https://*.quadim.ai https://*.cantara.no https://raw.githubusercontent.com 'unsafe-inline' 'unsafe-eval'; media-src 'none'; frame-src 'none'; font-src 'self' data: https://fonts.gstatic.com; connect-src 'self'; report-uri REDACTED");
		response.addHeader("X-Content-Type-Options", "nosniff");
		response.addHeader("X-Permitted-Cross-Domain-Policies", "master-only");
		response.addHeader("X-XSS-Protection", "1; mode=block");
		response.addHeader("X-Frame-Options", "deny");

	}

    public void setCSP2(HttpServletResponse response) {
		response.addHeader("Content-Security-Policy", "frame-ancestors 'none'");
		response.addHeader("Content-Security-Policy-Report-Only", "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' https://fonts.googleapis.com 'unsafe-inline' data:; img-src 'self' data: https://*.quadim.ai https://*.cantara.no https://raw.githubusercontent.com/ 'unsafe-inline' 'unsafe-eval'; media-src 'none'; frame-src 'none'; font-src 'self' data: https://fonts.gstatic.com; connect-src 'self'; report-uri REDACTED");
	}


    //HANDLE LOGIC FROM CONTROLLERS

	public String getCSRFtoken() {
		String csrftoken = UUID.randomUUID().toString();
		csrftokens.put(csrftoken, String.valueOf(System.currentTimeMillis()));
		return csrftoken;
	}
	
	private boolean isValidUUID(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

	public boolean validCSRFToken(String csrftoken) {
		boolean valid = false;
		if(csrftokens.containsKey(csrftoken)) {
			
			if(isValidUUID(csrftokens.get(csrftoken)) || (System.currentTimeMillis() - Long.valueOf(csrftokens.get(csrftoken)) >= 3*60*1000)) {
				csrftokens.remove(csrftoken);	
				return false;				
			}
			
			valid = true;
		}	
		return valid;
	}

	public WhydahServiceClient getServiceClient() {
		return serviceClient;
	}

	public String getAppLinks() {
		return ApplicationMapper.toShortListJson(serviceClient.getApplicationList());
	}
	
	public String getFullApplicationList() {
		return ApplicationMapper.toJson(serviceClient.getApplicationList());
	}
	
	public String getSafeJsonApplicationList() {
		return ApplicationMapper.toSafeJson(serviceClient.getApplicationList());
	}

	private boolean shouldUpdate() {
		int max = 100;
		return (5 >= ((int) (Math.random() * max)));  // update on 5 percent of requests
	}

	public void updateApplinks() {

		if (shouldUpdate()) {
			serviceClient.getWAS().updateApplinks();
		}
	}

	public String getApplicationID(String redirectURI) {
		if (redirectURI == null || redirectURI.length() < 10) {
			return null;
		}
		List<Application> applications = serviceClient.getApplicationList();
		if (applications != null) {
			for (Application app : applications) {               
				if (ServerUtil.compare(app.getApplicationUrl(), redirectURI)) {
					return app.getId();
				}
			}
		}
		return null;
	}
	
	public boolean checkIfUserExists(String username) {
		Boolean exists = new CommandUserExists(uasServiceUri, serviceClient.getMyAppTokenID(), UserTokenXpathHelper.getUserTokenId(getUserAdminTokenXml()), username).execute();
		return exists;
	}

	public boolean checkIfUserHasPassword(String username) {
		Boolean exists = new CommandUserPasswordLoginEnabled(uasServiceUri, serviceClient.getMyAppTokenID(), username).execute();
		return exists;
	}

//	public boolean checkIfUserNameExists(String username) {
//		Boolean exists = new CommandUserNameExists(uasServiceUri, serviceClient.getMyAppTokenID(), UserTokenXpathHelper.getUserTokenId(getUserAdminTokenXml()), username).execute();
//		return exists;
//	}
	
	public String getUserAdminTokenXml(String userTicket){

		log.trace("getUserAdminTokenId called - Calling UserAdminService at " + uasServiceUri + " userCredentialXml:" + adminUserCredential);
		try {
			String userTokenXml = serviceClient.getUserToken(adminUserCredential, userTicket);
			return userTokenXml;
		} catch (Exception e) {
			log.error("Problems getting userAdminTokenId", uasServiceUri);
			throw e;
		}

	}

	public String getUserRoleList(String userTokenId){
		String userTokenXml = serviceClient.getUserTokenByUserTokenID(userTokenId);
		String userid = UserTokenXpathHelper.getUserID(userTokenXml);
		UserToken adminUser = getUserAdminToken();
		String userRolesJson = new CommandGetUserRoles(uasServiceUri, serviceClient.getMyAppTokenXml(), adminUser.getUserTokenId(), userid).execute();
		log.debug("Roles returned:" + userRolesJson);
		return userRolesJson;
	}
	
	public String addUserRoleList(String userTokenId, String userRoleJson){
		
		
		String userTokenXml = serviceClient.getUserTokenByUserTokenID(userTokenId);
        String uId = UserXpathHelper.getUserIdFromUserTokenXml(userTokenXml);
        String userAddRoleResult = new CommandAddUserRole(uasServiceUri, serviceClient.getMyAppTokenID(), userTokenId, uId, userRoleJson).execute();
        log.debug("userAddRoleResult:{}", userAddRoleResult);
		return userAddRoleResult;
	}

	public boolean deleteUserRole(String userTokenId, String roleId){
		
		String userTokenXml = serviceClient.getUserTokenByUserTokenID(userTokenId);
        String uId = UserXpathHelper.getUserIdFromUserTokenXml(userTokenXml);
        boolean result = new CommandDeleteUserRole(uasServiceUri, serviceClient.getMyAppTokenID(), userTokenId, uId, roleId).execute();
		return result;
	}

	public String findUserTokenXMLFromSession(HttpServletRequest request, HttpServletResponse response, Model model){
		
		/* FROM totto's comment
		   we should probably look at shared function like "public static UserToken findUserTokenFromSession(httprequest,httpresponse)" 
		   which check and handle cookie(s) and ticket correct and smart...   
		   but it is not a good fit for SDK, as it need the servlet-api  (sdk is client-oriented)... 
		   and we probably have som "variants" in handling...    as I see that INNsolwa, ssolwa and uawa handle it differently...  
		   and inconsistently... 
		   and it is easy to do it wrong...    
		   I think ticket should win over cookie  (if ticket exist and is valid...  
		   but if invalid ticket, we should ignore the ticker and use the usertokenid from the cookie...   
		   if the ticket is ok, we should update the cookie with the new usertokenid.. 
		   and if the usertokenid from the cookie is invalid (id, answer invalif drom sts) we should delete the cookie  
		   (but not if we get exception i.e. if sts is down...)
		 */
		String userTicket = getFromRequest_UserTicket(request);
		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
		String userTokenXml = null ;

		CookieManager.addSecurityHTTPHeaders(response);
		boolean isValidTicket=false;
		
		try {
			//try ticket
			if (userTicket != null && userTicket.length() > 3) { 
				log.trace("Find UserToken - Using userTicket");
				userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTicket(userTicket);
				isValidTicket = userTokenXml!=null;
			}
			//if ticket is invalid, use cookie
			if (userTokenXml==null && userTokenId != null && userTokenId.length() > 3) { //from cookie
				log.trace("Find UserToken - Using userTokenID from cookie");
				userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
			}
			
			if(userTokenXml==null){				
				log.trace("Find UserToken - no session found");
				//delete cookie NOT WHEN STS is down
				if(ServerUtil.isServerOnline(tokenServiceUri.toString())){
					CookieManager.clearUserTokenCookies(request, response);
				}
			} else {
				//update cookie with a working usertokenid
				String tokenid =  UserTokenXpathHelper.getUserTokenId(userTokenXml);
				Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
				CookieManager.updateUserTokenCookie(tokenid, tokenRemainingLifetimeSeconds, request, response);
				//fill in model
				model.addAttribute(ConstantValue.USER_TOKEN_ID, tokenid);
				if(userTicket!=null && isValidTicket){
					model.addAttribute(ConstantValue.USERTICKET, userTicket);
				}
				return userTokenXml;
			}
			
			
		} catch (Exception e) {
			log.warn("welcome redirect - SecurityTokenException exception: ", e);
			return null;
		}
		return null;
	}

    public UserToken getUserAdminToken() {
        UserToken adminUser = UserTokenMapper.fromUserTokenXml(getUserAdminTokenXml());
        return adminUser;
    }

    private String adminUserTicket = null;
    private String adminUserTokenXML = null;

    public String getUserAdminTokenXml() {
        try {
            adminUserTicket = UUID.randomUUID().toString();
            adminUserTokenXML = serviceClient.getUserToken(adminUserCredential, adminUserTicket);
            return adminUserTokenXML;
        } catch (Exception e) {
            log.error("Problems getting userAdminTokenId", uasServiceUri);
            throw e;
        }
        //
        //        if (adminUserTokenXML != null) {
        //            return adminUserTokenXML;
        //        } else {
        //        	if(adminUserTicket==null){
        //        		adminUserTicket = UUID.randomUUID().toString();
        //        	}
        //            log.trace("getUserAdminTokenId called - Calling UserAdminService at " + uasServiceUri + " userCredentialXml:" + adminUserCredential);
        //            try {
        //                adminUserTokenXML = serviceClient.getUserToken(adminUserCredential, adminUserTicket);
        //                return adminUserTokenXML;
        //            } catch (Exception e) {
        //                log.error("Problems getting userAdminTokenId", uasServiceUri);
        //                throw e;
        //            }
        //        }


    }
	
	public String updateUserToken(UserToken ut) {
		UserIdentity u = new UserIdentity(ut.getUid(), ut.getUserName(), ut.getFirstName(), ut.getLastName(), ut.getPersonRef(), ut.getEmail(), ut.getCellPhone());
		String userjson = UserIdentityMapper.toJson(u);
		return new CommandUpdateUser(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), u.getUid(), userjson).execute();
	}

	public void addRedirectURIForNewUser(String username, String redirectURI) {
		if(redirectURI!=null && !redirectURI.equals(DEFAULT_REDIRECT)) {
			log.debug("add redirectURI= {} for username {}", redirectURI, username);
			username_redirectURI.put(username, redirectURI);
		}
	}
	
	public String getRedirectURIForNewUser(String username) {	
		String redirectURI = username_redirectURI.remove(username);
		log.debug("get redirectURI= {} for username {}", redirectURI, username);
		return redirectURI;
	}
	
	public void syncWhydahUserInfoWithThirdPartyUserInfo(String whydahuid, String provider, String accessToken, String appRoles, String thirdpartyUserId, String username, String firstName, String lastName, String email, String cellPhone, String personRef) {
		try {
			String json = new CommandGetUserAggregate(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), whydahuid).execute();
			log.warn("syncWhydahUserInfoWithThirdPartyUserInfo CommandGetUserAggregate -  json {}", json);
			if (json != null) {
				UserAggregate u = UserAggregateMapper.fromJson(json);
				log.warn("syncWhydahUserInfoWithThirdPartyUserInfo UserAggregate - {}", u);
				if (u != null) {
					u.setCellPhone(cellPhone);
					u.setEmail(email);
					u.setFirstName(firstName);
					u.setLastName(lastName);
					u.setPersonRef(personRef);
					boolean aad_data_found = false;
					boolean google_data_found = false;
					boolean rebel_data_found = false;
					for (UserApplicationRoleEntry entry : u.getRoleList()) {

						if (provider.equalsIgnoreCase("aad")) {
							if (entry.getOrgName().equalsIgnoreCase("AzureAD") && entry.getRoleName().equalsIgnoreCase("data")) {
								aad_data_found = true;
								String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("aad", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
								entry.setRoleValue(value);
							}
						}
						if (provider.equalsIgnoreCase("google")) {
							if (entry.getOrgName().equalsIgnoreCase("Google") && entry.getRoleName().equalsIgnoreCase("data")) {
								google_data_found = true;
								String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("google", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
								entry.setRoleValue(value);
							}
						}
						if (provider.equalsIgnoreCase("rebel")) {
							if (entry.getOrgName().equalsIgnoreCase("Rebel") && entry.getRoleName().equalsIgnoreCase("data")) {
								rebel_data_found = true;
								String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("rebel", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
								entry.setRoleValue(value);
							}
						}
						//do for facebook
						//do for netiq
						//do for others

					}
					if (!aad_data_found && provider.equalsIgnoreCase("aad")) {
						UserApplicationRoleEntry role = new UserApplicationRoleEntry();
						role.setUserId(whydahuid);
						role.setApplicationId("2215");
						role.setApplicationName("Whydah");
						role.setOrgName("AzureAD");
						role.setRoleName("data");
						String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("aad", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
						role.setRoleValue(value);
						if (u.getRoleList() == null) {
							u.setRoleList(new ArrayList<UserApplicationRoleEntry>());
						}
						u.getRoleList().add(role);

					}

					if (!google_data_found && provider.equalsIgnoreCase("google")) {
						UserApplicationRoleEntry role = new UserApplicationRoleEntry();
						role.setUserId(whydahuid);
						role.setApplicationId("2215");
						role.setApplicationName("Whydah");
						role.setOrgName("Google");
						role.setRoleName("data");
						String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("google", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
						role.setRoleValue(value);
						if (u.getRoleList() == null) {
							u.setRoleList(new ArrayList<UserApplicationRoleEntry>());
						}
						u.getRoleList().add(role);

					}
					
					if (!rebel_data_found && provider.equalsIgnoreCase("rebel")) {
						UserApplicationRoleEntry role = new UserApplicationRoleEntry();
						role.setUserId(whydahuid);
						role.setApplicationId("2215");
						role.setApplicationName("Whydah");
						role.setOrgName("Rebel");
						role.setRoleName("data");
						String value = Base64.getEncoder().encodeToString(("<![CDATA[" + serviceClient.getUserXml("rebel", accessToken, appRoles, thirdpartyUserId, firstName, lastName, username, email, cellPhone, personRef) + "]]>").getBytes());
						role.setRoleValue(value);
						if (u.getRoleList() == null) {
							u.setRoleList(new ArrayList<UserApplicationRoleEntry>());
						}
						u.getRoleList().add(role);

					}

					new CommandUpdateUserAggregate(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), UserAggregateMapper.toJson(u)).execute();
				}
			}
			log.warn("syncWhydahUserInfoWithThirdPartyUserInfo UserAggregate -  returned null");
		} catch (Exception e) {
			log.error("Unable to sync state with whydah", e);
		}


	}

	public void saveRoleDatatoWhydah(String whydahuid, String appId, String appName, String orgName, String roleName, String roleValue) {
		log.info("saveRoleDatatoWhydah start ");
		try {
			String json = new CommandGetUserAggregate(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), whydahuid).execute();
			log.info("saveRoleDatatoWhydah - returned json {}", json);
			if (json != null) {
				UserAggregate u = UserAggregateMapper.fromJson(json);
				log.info("saveRoleDatatoWhydah - returned UserAggregate {}", u);
				boolean role_found = false;
				for (UserApplicationRoleEntry entry : u.getRoleList()) {
					if (entry.getRoleName().equalsIgnoreCase(roleName)) {
						log.info("saveRoleDatatoWhydah - role_found {}", role_found);
						role_found = true;
						entry.setRoleValue(roleValue);
					}
				}
				if (!role_found) {
					log.info("saveRoleDatatoWhydah - create role ");
					UserApplicationRoleEntry role = new UserApplicationRoleEntry();
					role.setUserId(whydahuid);
					role.setApplicationId(appId);
					role.setApplicationName(appName);
					role.setOrgName(orgName);
					role.setRoleName(roleName);
					role.setRoleValue(roleValue);
					if (u.getRoleList() == null) {
						u.setRoleList(new ArrayList<UserApplicationRoleEntry>());
					}
					u.getRoleList().add(role);
				}
				new CommandUpdateUserAggregate(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), UserAggregateMapper.toJson(u)).execute();
				log.info("saveRoleDatatoWhydah end ");
			}
			log.info("saveRoleDatatoWhydah - returned json null");
		} catch (Exception e) {
			log.error("Unable to update Whydah", e);
		}

	}
	
	public String saveWhydahRole(String payload) {
		UserApplicationRoleEntry entry = UserRoleMapper.fromJson(payload);	
		String json = new CommandGetUserRoles(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), entry.getUserId()).execute();
		List<UserApplicationRoleEntry> list = UserRoleMapper.fromJsonAsList(json);
		UserApplicationRoleEntry found = null;
		for(UserApplicationRoleEntry item : list) {
			if(item.getApplicationId().equalsIgnoreCase(entry.getApplicationId()) && 
					item.getOrgName().equalsIgnoreCase(entry.getOrgName()) &&
					item.getRoleName().equalsIgnoreCase(entry.getRoleName())) {
				found = item;
				break;
			}
		}
		if(found!=null) {
			found.setOrgName(entry.getOrgName());
			found.setRoleValue(entry.getRoleValue());
			return new CommandUpdateUserRole(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), entry.getUserId(), found.getId(), UserRoleMapper.toJson(found)).execute();
		} else {
			return new CommandAddUserRole(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), entry.getUserId(), payload).execute();
		}
	}

	public Boolean deleteWhydahRole(String uid, String roleId) {
		return new CommandDeleteUserRole(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), uid, roleId).execute();
	}

	public String findWhydahRole(String uid, String appid, String orgname, String rolename) throws JsonProcessingException {
		List<UserApplicationRoleEntry> foundList = new ArrayList<UserApplicationRoleEntry>();
		if(uid!=null) {
			String json = new CommandGetUserRoles(uasServiceUri, serviceClient.getMyAppTokenID(), getUserAdminToken().getUserTokenId(), uid).execute();
			List<UserApplicationRoleEntry> list = UserRoleMapper.fromJsonAsList(json);

			for(UserApplicationRoleEntry item : list) {
				if((appid==null || item.getApplicationId().equalsIgnoreCase(appid)) && 
						(orgname ==null || item.getOrgName().equalsIgnoreCase(orgname)) &&
						(rolename == null || item.getRoleName().equalsIgnoreCase(rolename))) {
					foundList.add(item);
				}
			}
		}
		return objectMapper.writeValueAsString(foundList);
	}

	public void addOIDCProvider(Provider provider) {
		enabledLoginTypes.addOIDCProvider(provider);
	}

	public Set<Provider> getOIDCProvidersEnabled() {
		return enabledLoginTypes.getOIDCProvidersEnabled();
	}
}
