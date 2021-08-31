package net.whydah.sso.authentication;

import net.whydah.sso.config.AppConfig;
import net.whydah.sso.ddd.model.user.UserTokenId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;


public class CookieManager {
    public static String USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso";
    private static final Logger log = LoggerFactory.getLogger(CookieManager.class);
    private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    private static String cookiedomain = null;
    private static String MY_APP_URI;
    private static boolean IS_MY_URI_SECURED = false;

    private CookieManager() {
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
            USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso_" + MY_APP_URI
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

    public static void addSecurityHTTPHeaders(HttpServletResponse response) {
        //TODO Vi trenger en plan her.
        //response.setHeader("X-Frame-Options", "sameorigin");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    }


    public static void createAndSetUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        cookie.setValue(userTokenId);

        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }

        addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, request, response);
    }

    public static void updateUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if(cookie==null){
        	cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        }
        addCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);
    }

//    private static void updateCookie(Cookie cookie, String cookieValue, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
//        if (cookieValue != null) {
//            cookie.setValue(cookieValue);
//        }
//        //Only name and value are sent back to the server from the browser. The other attributes are only used by the browser to determine of the cookie should be sent or not.
//        //http://en.wikipedia.org/wiki/HTTP_cookie#Setting_a_cookie
//        if (tokenRemainingLifetimeSeconds == null) {
//            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
//        }
//
//        //addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, request, response);
//        updateCookie2(cookie, cookie.getValue(), tokenRemainingLifetimeSeconds, response);
//    }
    
    public static void clearUserTokenCookies(HttpServletRequest request, HttpServletResponse response) {
    	Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, "");
    	cookie.setMaxAge(0);
    	cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static String getUserTokenId(HttpServletRequest request) {
        String userTokenId = request.getParameter(CookieManager.USER_TOKEN_REFERENCE_NAME);
        if (userTokenId != null && userTokenId.length() > 1) {
            log.warn("getUserTokenId: userTokenIdFromRequest={}", userTokenId);
        } else {
            userTokenId = CookieManager.getUserTokenIdFromCookie(request);
            log.warn("getUserTokenId: userTokenIdFromCookie={}", userTokenId);
        }
        return userTokenId;
    }

    public static String getUserTokenIdFromCookie(HttpServletRequest request) {
        Cookie userTokenCookie = getUserTokenCookie(request);
        return (userTokenCookie != null ? userTokenCookie.getValue() : null);
    }

    private static Cookie getUserTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            log.debug("getUserTokenCookie: cookie with name={}, value={}", cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
            if (USER_TOKEN_REFERENCE_NAME.equalsIgnoreCase(cookie.getName())) {
                log.debug("Found and returning matching cookie - with name={}, value={}", cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
                return cookie;
            }
        }
        return null;
    }

//    private static void updateCookie2(Cookie cookie, String cookieValue, Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
//        if (cookieValue != null) {
//            cookie.setValue(cookieValue);
//        }
//        //Only name and value are sent back to the server from the browser. The other attributes are only used by the browser to determine of the cookie should be sent or not.
//        //http://en.wikipedia.org/wiki/HTTP_cookie#Setting_a_cookie
//
//        if (tokenRemainingLifetimeSeconds == null) {
//            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
//        }
//        cookie.setMaxAge(tokenRemainingLifetimeSeconds);
//
//        if (cookiedomain != null && !cookiedomain.isEmpty()) {
//            cookie.setDomain(cookiedomain);
//        }
//        cookie.setPath("; HttpOnly;");
//        cookie.setSecure(IS_MY_URI_SECURED);
//        log.debug("Created/updated cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
//        response.addCookie(cookie);
//    }

    private static void addCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        String DATE_FORMAT = "EEE, dd-MMM-yyyy hh:mm:ss z";
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String expiresDateString = dateFormat.format(Date.from(Instant.now().plus(tokenRemainingLifetimeSeconds, ChronoUnit.SECONDS)));
        if (UserTokenId.isValid(userTokenId)) {
            Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
            cookie.setPath("/");
            cookie.setMaxAge(tokenRemainingLifetimeSeconds);
            cookie.setSecure(IS_MY_URI_SECURED);
            response.addCookie(cookie);
        	/*
            StringBuilder sb = new StringBuilder(USER_TOKEN_REFERENCE_NAME);
            sb.append("=");
            sb.append(userTokenId);
            sb.append(";expires=");
            sb.append(expiresDateString);
            sb.append(";path=");
            sb.append("/");
            sb.append(";HttpOnly");
            if (IS_MY_URI_SECURED) {
                sb.append(";secure");
            }
            response.setHeader("SET-COOKIE", sb.toString());
            */

        } else {
            StringBuilder sb = new StringBuilder(USER_TOKEN_REFERENCE_NAME);
//            sb.append("=");
            sb.append(userTokenId);
            sb.append(";expires=");
//            sb.append("0");
            sb.append(";path=");
            sb.append("/");
            sb.append(";HttpOnly");
            if (IS_MY_URI_SECURED) {
                sb.append(";secure");
            }
            log.trace("Cleared cookie " + sb);
            response.setHeader("SET-COOKIE", sb.toString());

/*
            Cookie cookie = getUserTokenCookie(request);
            if (cookie == null) {
                cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
            }
            cookie.setValue("");
            cookie.setMaxAge(0);
            if (cookiedomain != null && !cookiedomain.isEmpty()) {
                cookie.setDomain(cookiedomain);
            }
            cookie.setPath(";HttpOnly");
            cookie.setSecure(IS_MY_URI_SECURED);
            response.addCookie(cookie);
            log.trace("Cleared cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
*/
        }
    }
}
