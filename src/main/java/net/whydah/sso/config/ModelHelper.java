//package net.whydah.sso.config;
//
//import net.whydah.sso.config.AppConfig;
//import net.whydah.sso.config.LoginTypes;
//
//import org.springframework.ui.Model;
//
//import java.io.IOException;
//
//public class ModelHelper {
//
//    public static LoginTypes enabledLoginTypes;
//    public static final String USERTICKET = "userticket";
//    public static final String USERTOKEN = "usertoken";
//    public static final String CRMCUSTOMER = "CRMcustomer";
//    public static final String USERACTIVITIES = "useractivities";
//    public static final String USERACTIVITIES_SIMPLIFIED = "useractivities_simple";
//
//    public static final String REALNAME = "realname";
//    public static final String USER_TOKEN_ID = "usertokenid";
//    public static final String PHONE_NUMBER = "phonenumber";
//    public static final String SECURITY_LEVEL = "securitylevel";
//    public static final String EMAIL = "email";
//    public static final String DEFCON = "defcon";
//    public static final String PERSON_REF = "personRef";
//    public static final String JSON_DATA = "jsondata";
//
//    public static final String FACEBOOKLOGINENABLED = "facebookLoginEnabled";
//    public static final String NETIQLOGINENABLED = "netIQLoginEnabled";
//    public static final String OPENIDLOGINENABLED = "openidLoginEnabled";
//    public static final String SIGNUPENABLED = "signupEnabled";
//    public static String INN_ROLE="INNData";
//    
//    static {
//        try {
//            enabledLoginTypes = new LoginTypes(AppConfig.readProperties());
//        } catch (IOException e) {
//            enabledLoginTypes = null;     
//        }
//    }
//            
//    
//    public static void setEnabledLoginTypes(Model model) {
//        model.addAttribute("signupEnabled", enabledLoginTypes.isSignupEnabled());
//        model.addAttribute("facebookLoginEnabled", enabledLoginTypes.isFacebookLoginEnabled());
//        model.addAttribute("openidLoginEnabled", enabledLoginTypes.isOpenIdLoginEnabled());
//        model.addAttribute("omniLoginEnabled", enabledLoginTypes.isOmniLoginEnabled());
//        model.addAttribute("netIQLoginEnabled", enabledLoginTypes.isNetIQLoginEnabled());
//        model.addAttribute("userpasswordLoginEnabled", enabledLoginTypes.isUserpasswordLoginEnabled());
//
//        if (enabledLoginTypes.isNetIQLoginEnabled()) {
//            setNetIQOverrides(model);
//        }
//    }
//
//    public static boolean isEnabled(String loginType) {
//        if (loginType.equalsIgnoreCase(FACEBOOKLOGINENABLED)) {
//            return enabledLoginTypes.isFacebookLoginEnabled();
//        }
//        if (loginType.equalsIgnoreCase("openidLoginEnabled")) {
//            return enabledLoginTypes.isOpenIdLoginEnabled();
//        }
//        if (loginType.equalsIgnoreCase("omniLoginEnabled")) {
//            return enabledLoginTypes.isOmniLoginEnabled();
//        }
//        if (loginType.equalsIgnoreCase(NETIQLOGINENABLED)) {
//            return enabledLoginTypes.isNetIQLoginEnabled();
//        }
//        if (loginType.equalsIgnoreCase(SIGNUPENABLED)) {
//            return enabledLoginTypes.isSignupEnabled();
//        }
//        return false;
//    }
//    private static void setNetIQOverrides(Model model) {
//        try {
//            model.addAttribute("netIQtext", AppConfig.readProperties().getProperty("logintype.netiq.text"));
//            model.addAttribute("netIQimage", AppConfig.readProperties().getProperty("logintype.netiq.logo"));
//        } catch (IOException ioe) {
//            model.addAttribute("netIQtext", "NetIQ");
//            model.addAttribute("netIQimage", "images/netiqlogo.png");
//        }
//    }
//}