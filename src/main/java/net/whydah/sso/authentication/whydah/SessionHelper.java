package net.whydah.sso.authentication.whydah;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionHelper {
    public static final String USERTICKET = "userticket";
    public static final String REDIRECT = "redirect";
    public static final String REDIRECT_URI = "redirectURI";
    public static final String SESSIONCHECK = "SessionCheck";
    public static final String LOGO_URL = "logoURL";
    public static final String APP_LINKS = "appLinks";
    public static final String LOGIN_ERROR = "loginError";
    public static final String IAM_MODE = "iammode";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String WHYDAH_VERSION = "version";
    public static final String CSRFtoken = "CSRFtoken";

    private static Map<String, String> csrftokens = new HashMap<>();


    public static String getCSRFtoken() {
        String csrftoken = UUID.randomUUID().toString();
        csrftokens.put(csrftoken, csrftoken);
        return csrftoken;
    }

    public static boolean validCSRFToken(String csrftoken) {
        String token = csrftokens.get(csrftoken);
        if (token != null && token.length() > 4) {
            return true;
        }
        return false;
    }

}
