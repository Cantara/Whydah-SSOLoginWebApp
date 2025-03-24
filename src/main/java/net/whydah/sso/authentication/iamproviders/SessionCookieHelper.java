package net.whydah.sso.authentication.iamproviders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whydah.sso.config.AppConfig;

public class SessionCookieHelper {
	public static String REFERENCE_NAME = "auth_sessionid";
	private static final Logger log = LoggerFactory.getLogger(SessionCookieHelper.class);
	private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;
	private static String MY_APP_URI;
    private static boolean IS_MY_URI_SECURED = false;
    private static String cookiedomain = null;
    
    private SessionCookieHelper() {
    }

    static {
        try {
            cookiedomain = AppConfig.readProperties().getProperty("cookiedomain");
            MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
           
            URL uri;
            if (MY_APP_URI != null) {

                uri = new URL(MY_APP_URI);
                IS_MY_URI_SECURED = MY_APP_URI.indexOf("https") >= 0;
                if (cookiedomain == null || cookiedomain.isEmpty()) {
                    String domain = uri.getHost();
                    domain = domain.startsWith("www.") ? domain.substring(4) : domain;
                    cookiedomain = domain;
                }
            }
            REFERENCE_NAME = "auth_sessionid_" + MY_APP_URI
                    .replace("https://", "")
                    .replace("http://", "")
                    .replace(":", "")
                    .replace("?", "")
                    .replace("&", "")
                    .replace("/", "_");

        } catch (IOException e) {
            log.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
        }
    }
    
	/*
	 * 
	 * 
	 * 
	 * we should add the sessionid to a cookie which can be shared/reused on other server instances. Imagine we fall into this scenario
		SSOLWA-1 handles the google redirect call and establishes a session (sessonid, sessiondata) and calls the "confirm" page
		the POST "confirm" request is then handled in SSOLWA-2 - the one has no knowledge of the current session that SSOLWA-1 is processing 
		
		So we may use a shared cookie that contains the sessionid incase SSOLWA-2, SOOLWA-3+++ want to take the further steps with the authentication process
		
		That scenario can explain why you see the user has to click "confirm" twice

	 */
	public static void addSessionCookie(String iam_provider, String sessionid, HttpServletResponse response) {
		
        String DATE_FORMAT = "EEE, dd-MMM-yyyy hh:mm:ss z";
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        //String expiresDateString = dateFormat.format(Date.from(Instant.now().plus(DEFAULT_COOKIE_MAX_AGE, ChronoUnit.SECONDS)));
        StringBuilder sb = new StringBuilder(getCookieReferenceNameForAuthProvider(iam_provider));
        sb.append("=");
        sb.append(sessionid);
        sb.append(";expires=");
        sb.append(-1);
        sb.append(";path=");
        sb.append("/");
        sb.append(";HttpOnly");
        if (IS_MY_URI_SECURED) {
            sb.append(";secure");
        }
        response.setHeader("SET-COOKIE", sb.toString());
	}
	
	public static void clearSessionCookie(String iam_provider, HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(getCookieReferenceNameForAuthProvider(iam_provider), "");
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(cookie);
		
		Cookie sessionCookie = new Cookie("JSESSIONID", "");
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(sessionCookie);
		
		Cookie sessionCookie2 = new Cookie("JSESSIONID", "");
		cookie.setMaxAge(0);
		cookie.setPath("/sso");
		response.addCookie(sessionCookie2);
		
		
	}
	
	public static String getSessionCookie(String iam_provider, HttpServletRequest request) {
		
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		String name = getCookieReferenceNameForAuthProvider(iam_provider);
		String val = null;
		for (Cookie cookie : cookies) {
			log.debug("getUserTokenCookie: cookie with name={}, value={}", cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
			if (name.equalsIgnoreCase(cookie.getName())) {
				log.debug("Found and returning matching cookie - with name={}, value={}", cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
				val = cookie.getValue();
				break;
			}
		}
		return val!=null && !val.isEmpty()? val: null;
	}

	
	public static String getCookieReferenceNameForAuthProvider(String iam_provider) {
		return iam_provider + "_" + REFERENCE_NAME;
	}

}
