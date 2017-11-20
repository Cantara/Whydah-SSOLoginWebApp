package net.whydah.sso.dao;

import net.whydah.sso.ServerRunner;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.commands.adminapi.user.CommandUserExists;
import net.whydah.sso.commands.adminapi.user.CommandUserPasswordLoginEnabled;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandDeleteUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandGetUserRoles;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.LoginTypes;
import net.whydah.sso.ddd.model.RedirectURI;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.utils.ServerUtil;
import net.whydah.sso.utils.SignupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
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


	public CRMHelper getCRMHelper(){
		return crmHelper;
	}

	public ReportServiceHelper getReportServiceHelper(){
		return reportServiceHelper;
	}
	
	private Map<String, String> csrftokens = new HashMap<>();

	private SessionDao() {

		try {

			properties = AppConfig.readProperties();
			this.LOGOURL = properties.getProperty("logourl", LOGOURL);
			MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
            LOGOUT_ACTION_URI = MY_APP_URI + "logoutaction";
            LOGIN_URI = MY_APP_URI + "login";
			
			this.serviceClient = new WhydahServiceClient(properties);
			this.tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
			this.uasServiceUri = UriBuilder.fromUri(properties.getProperty("useradminservice")).build();
			this.crmServiceUri = UriBuilder.fromUri(properties.getProperty("crmservice")).build();
			this.reportservice = UriBuilder.fromUri(properties.getProperty("reportservice")).build();
			this.adminUserCredential = new UserCredential(properties.getProperty("uasuser"), properties.getProperty("uaspw"));
			crmHelper = new CRMHelper(serviceClient, crmServiceUri, properties.getProperty("email.verification.link"));
			reportServiceHelper = new ReportServiceHelper(serviceClient, reportservice);

            this.matchRedirectURLtoModel = Boolean.getBoolean(properties.getProperty("matchRedirects"));
        } catch (IOException e) {
			e.printStackTrace();
		}

	}



	//////ADD BASIC VALUE TO THE MODEL

	public Model addModel_LoginTypes(Model model){
		LoginTypes enabledLoginTypes = new LoginTypes(properties);
		model.addAttribute(ConstantValue.SIGNUPENABLED, enabledLoginTypes.isSignupEnabled());
		model.addAttribute(ConstantValue.USERPASSWORDLOGINENABLED, enabledLoginTypes.isUserpasswordLoginEnabled());
        model.addAttribute(ConstantValue.FACEBOOKLOGIN_ENABLED, enabledLoginTypes.isFacebookLoginEnabled());
        model.addAttribute(ConstantValue.OPENIDLOGIN_ENABLED, enabledLoginTypes.isOpenIdLoginEnabled());
        model.addAttribute(ConstantValue.OMNILOGIN_ENABLED, enabledLoginTypes.isOmniLoginEnabled());
        model.addAttribute(ConstantValue.NETIQLOGIN_ENABLED, enabledLoginTypes.isNetIQLoginEnabled());

        if (enabledLoginTypes.isNetIQLoginEnabled()) {
            setNetIQOverrides(model);
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
		if (!(RedirectURI.isValid(redirectURI))) {
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

	
	//////END HANDLE PARAMTERS FROM THE REQUEST


    ///  Block side-channel attacks - looks correct bud does not clear the detectify "alert"
    public void setCSP(HttpServletResponse response) {
        response.addHeader("Content-Security-Policy", "frame-ancestors 'none'");
        response.addHeader("Content-Security-Policy-Report-Only", "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://*.opplysningen.no 'unsafe-inline' 'unsafe-eval'; media-src 'none'; frame-src 'none'; font-src 'self'; connect-src 'self'; report-uri REDACTED");
        response.addHeader("X-Content-Type-Options", "nosniff");
        response.addHeader("X-Permitted-Cross-Domain-Policies", "master-only");
        response.addHeader("X-XSS-Protection", "1; mode=block");
        response.addHeader("X-Frame-Options", "deny");

    }

    public void setCSP2(HttpServletResponse response) {
        response.addHeader("Content-Security-Policy", "frame-ancestors 'none'");
        response.addHeader("Content-Security-Policy-Report-Only", "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://*.opplysningen.no 'unsafe-inline' 'unsafe-eval'; media-src 'none'; frame-src 'none'; font-src 'self'; connect-src 'self'; report-uri REDACTED");
    }


    //HANDLE LOGIC FROM CONTROLLERS

	public String getCSRFtoken() {
		String csrftoken = UUID.randomUUID().toString();
		csrftokens.put(csrftoken, csrftoken);
		return csrftoken;
	}

	public boolean validCSRFToken(String csrftoken) {
		return csrftokens.containsKey(csrftoken);
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

	public UserToken getUserAdminToken(){
		UserToken adminUser = UserTokenMapper.fromUserTokenXml(getUserAdminTokenXml());
		return adminUser;
	}

	public String getUserAdminTokenXml(){

		return getUserAdminTokenXml( UUID.randomUUID().toString());
		
	}
	
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
	



}
