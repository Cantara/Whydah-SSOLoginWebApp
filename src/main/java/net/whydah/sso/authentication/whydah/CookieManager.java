package net.whydah.sso.authentication.whydah;

import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CookieManager {
    public static final String USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso";
    //private static final String LOGOUT_COOKIE_VALUE = "logout";
    private static final Logger log = LoggerFactory.getLogger(CookieManager.class);
    private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    private static String cookiedomain = null;
    private static String MY_APP_URI;

    private CookieManager() {
    }

    static {
        try {
            cookiedomain = AppConfig.readProperties().getProperty("cookiedomain");
            MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
        } catch (IOException e) {
            log.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
        }
    }

    public static void addSecurityHTTPHeaders(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "sameorigin");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    }

        public static void createAndSetUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds,HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        cookie.setValue(userTokenId);

        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
        cookie.setMaxAge(tokenRemainingLifetimeSeconds);

        if (cookiedomain != null && !cookiedomain.isEmpty()) {
            cookie.setDomain(cookiedomain);
        }
        cookie.setPath("/");
        if (!ApplicationMode.getApplicationMode().equals(ApplicationMode.TEST_L)) {
            cookie.setSecure(true);
        }
        if ("https".equalsIgnoreCase(request.getScheme())) {
            cookie.setSecure(true);
        } else {
            log.warn("Unsecure session detected, using myuri to define coocie security");
            cookie.setSecure(secureCookie(MY_APP_URI));

        }

        log.debug("Created cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
        response.addCookie(cookie);
    }

    public static void clearUserTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if (cookie != null) {
            cookie.setValue("");
            cookie.setMaxAge(0);
            if (cookiedomain != null && !cookiedomain.isEmpty()) {
                cookie.setDomain(cookiedomain);
            }
            cookie.setPath("/");
            if ("https".equalsIgnoreCase(request.getScheme())) {
                cookie.setSecure(true);
            } else {
                log.warn("Unsecure session detected, using myuri to define coocie security");
                cookie.setSecure(secureCookie(MY_APP_URI));

            }
            response.addCookie(cookie);
            log.trace("Cleared cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
        }
    }

    public static String getUserTokenId(HttpServletRequest request) {
        String userTokenId = request.getParameter(CookieManager.USER_TOKEN_REFERENCE_NAME);
        if (userTokenId != null && userTokenId.length() > 1) {
            log.debug("getUserTokenId: userTokenIdFromRequest={}", userTokenId);
        } else {
            userTokenId = CookieManager.getUserTokenIdFromCookie(request);
            log.debug("getUserTokenId: userTokenIdFromCookie={}", userTokenId);
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
                return cookie;
            }
        }
        return null;
    }

    public static boolean secureCookie(String myuri) {
        return myuri.indexOf("https") >= 0;
    }

    /*
    public static void setLogoutUserTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie userTokenCookie = getUserTokenCookie(request);
        if (userTokenCookie != null) {
            log.debug("Setting logout value on userToken cookie.");
            userTokenCookie.setValue(LOGOUT_COOKIE_VALUE);
            response.addCookie(userTokenCookie);
        }
    }
    */
}
