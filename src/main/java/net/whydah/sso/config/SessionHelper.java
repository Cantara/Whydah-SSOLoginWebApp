//package net.whydah.sso.config;
//
//import net.whydah.sso.application.mappers.ApplicationMapper;
//import net.whydah.sso.commands.adminapi.application.CommandListApplications;
//import net.whydah.sso.user.mappers.UserTokenMapper;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.http.HttpServletRequest;
//
//import java.net.URI;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//public class SessionHelper {
//    private static final Logger log = LoggerFactory.getLogger(SessionHelper.class);
//
//    public static final String USERTICKET = "userticket";
//    public static final String REDIRECT = "redirect";
//    public static final String REDIRECT_URI = "redirectURI";
//    public static final String SESSIONCHECK = "SessionCheck";
//    public static final String LOGO_URL = "logoURL";
//    public static final String APP_LINKS = "appLinks";
//    public static final String LOGIN_ERROR = "loginError";
//    public static final String IAM_MODE = "iammode";
//    public static final String USER = "user";
//    public static final String PASSWORD = "password";
//    public static final String WHYDAH_VERSION = "version";
//    public static final String CSRFtoken = "CSRFtoken";
//
//    public static final String DEFAULT_REDIRECT = "welcome";
//
//    public static final String CELL_PHONE = "cellphone";
//    public static final String APP_NAMES = "appNames";
//
//	public static final String ADDRESS = "address";
//	public static final String APPLICATIONNAME = "appName";
//	public static final String APPLICATIONID = "appId";
//	
//    public static String getAppLinks() {
//        return myAppLinks;
//    }
//
//    public static void setAppLinks(String appLinks) {
//        myAppLinks = appLinks;
//    }
//
//    private static Map<String, String> csrftokens = new HashMap<>();
//    private static String myAppLinks = "[{}]";
//
//
//    public static String getCSRFtoken() {
//        String csrftoken = UUID.randomUUID().toString();
//        csrftokens.put(csrftoken, csrftoken);
//        return csrftoken;
//    }
//
//    public static boolean validCSRFToken(String csrftoken) {
////        String token = csrftokens.get(csrftoken);
////        if (token != null && token.length() > 4) {
////            return true;
////        }
////        return false;
//    	return true;
//    }
//
//    public static void updateApplinks(URI userAdminServiceUri, String myAppTokenId, String responseXML) {
//        if (shouldUpdate() || getAppLinks() == null || myAppLinks.length() < 6) {
//            String userTokenId = UserTokenMapper.fromUserTokenXml(responseXML).getTokenid();
//            String applicationsJson = new CommandListApplications(userAdminServiceUri, myAppTokenId).execute();
//            log.debug("AppLications returned:" + applicationsJson);
//            if (applicationsJson != null) {
//                if (applicationsJson.length() > 20) {
//                    setAppLinks(ApplicationMapper.toShortListJson(ApplicationMapper.fromJsonList(applicationsJson)));
//
//                }
//            }
//        }
//    }
//
//    public static String getRedirectURI(HttpServletRequest request) {
//        String redirectURI = request.getParameter(SessionHelper.REDIRECT_URI);
//        //log.trace("getRedirectURI - redirectURI from request: {}", redirectURI);
//        if (redirectURI == null || redirectURI.length() < 1) {
//            log.trace("getRedirectURI - No redirectURI found, setting to {}", DEFAULT_REDIRECT);
//            return DEFAULT_REDIRECT;
//        }
//        try {
//            // TODO  Implement RedirectURI verification/swap here (from AppLinks)
//
//            URI redirect = new URI(redirectURI);
//            return redirectURI;
//        } catch (Exception e) {
//            return DEFAULT_REDIRECT;
//        }
//    }
//
//    public static boolean shouldUpdate() {
//        int max = 100;
//        return (5 >= ((int) (Math.random() * max)));  // update on 5 percent of requests
//    }
//
//}
