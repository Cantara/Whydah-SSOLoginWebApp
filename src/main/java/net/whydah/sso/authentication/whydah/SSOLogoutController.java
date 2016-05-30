package net.whydah.sso.authentication.whydah;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SSOLogoutController {
    private static final Logger log = LoggerFactory.getLogger(SSOLogoutController.class);
//    private final WhydahServiceClient tokenServiceClient;
//    private String MY_APP_URI;
//      private String LOGOUT_ACTION_URI;
//    private String LOGIN_URI;



    public SSOLogoutController() throws IOException {
//        this.tokenServiceClient = new WhydahServiceClient();
//        try {
//              MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
//              LOGOUT_ACTION_URI = SessionDao.instance.MY_APP_URI + "logoutaction";
//            LOGIN_URI = MY_APP_URI + "login";
//        } catch (IOException e) {
//            log.warn("Could not read myuri from properties. {}", e.getMessage());
//            MY_APP_URI = null;
//            LOGOUT_ACTION_URI = null;
//        }
    }

    /**
     * This is the endpoint clients should use.
     */
    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Model model) {
        //model.addAttribute("logoURL", getLogoUrl());
    	SessionDao.instance.addModel_LOGO_URL(model);
        String redirectUriFromClient = getRedirectUri(request);
        //model.addAttribute(SessionHelper.REDIRECT_URI, redirectUri);
        String userTokenId = CookieManager.getUserTokenId(request);
        log.trace("logout was called. userTokenId={}, redirectUriFromClient={}", userTokenId, redirectUriFromClient);

        if (userTokenId != null && userTokenId.length() > 3) {
            //Have userTokenId and can perform logout
            //model.addAttribute("TokenID", userTokenId);
            //ED: I think this is never called. Unkown what/when it should be used.
            //log.info("logout was called. userTokenIdFromRequest={}, redirectUri={}. TALK TO Erik if you see this log statement!", userTokenId, redirectUriFromClient);
            SessionDao.instance.getServiceClient().releaseUserToken(userTokenId);
            CookieManager.clearUserTokenCookies(request, response);
            String loginRedirectUri =  SessionDao.instance.LOGIN_URI + "?" + ConstantValue.REDIRECT_URI + "=" + redirectUriFromClient;
            model.addAttribute(ConstantValue.REDIRECT, loginRedirectUri);
            log.info("logout - Redirecting to loginRedirectUri={}", loginRedirectUri);
            return "action";
        } else {
            //No userTokenId, so not possible to perform logout.  Redirect to logoutaction to ensure browser sets the expected cookie on the request to obtian the userTokenId.
            String logout_action_redirect_uri = SessionDao.instance.LOGOUT_ACTION_URI + "?" + ConstantValue.REDIRECT_URI + "=" + redirectUriFromClient;
            model.addAttribute(ConstantValue.REDIRECT, logout_action_redirect_uri);
            //model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
            log.info("logout - Redirecting to LOGOUT_ACTION_URI={}", logout_action_redirect_uri);
            return "action";
        }
    }

    /**
     * This endpoint is only used for performing a redirect to fetch the clients cookie.
     */
    @RequestMapping("/logoutaction")
    public String logoutAction(HttpServletRequest request, HttpServletResponse response, Model model) {
        String userTokenId = CookieManager.getUserTokenId(request);
        String redirectUri = getRedirectUri(request);
        log.trace("logoutaction was called. userTokenId={}, redirectUri={}", userTokenId, redirectUri);

        if (userTokenId != null && userTokenId.length() > 1) {
            SessionDao.instance.getServiceClient().releaseUserToken(userTokenId);
        } else {
            log.warn("logoutAction - tokenServiceClient.releaseUserToken was not called because no userTokenId was found in request or cookie.");
        }
        CookieManager.clearUserTokenCookies(request, response);
        //ED: Why
        //CookieManager.setLogoutUserTokenCookie(request, response);

        model.addAttribute("logoURL", getLogoUrl());
        String loginRedirectUri = SessionDao.instance.LOGIN_URI + "?" + ConstantValue.REDIRECT_URI + "=" + redirectUri;
        model.addAttribute(ConstantValue.REDIRECT, loginRedirectUri);
        log.info("logoutaction - Redirecting to loginRedirectUri={}", loginRedirectUri);
        return "action";
    }

    private String getLogoUrl() {
        String LOGOURL = "/sso/images/site-logo.png";
        try {
            Properties properties = AppConfig.readProperties();
            LOGOURL = properties.getProperty("logourl");
        } catch (Exception e) {
            log.error("", e);
        }
        return LOGOURL;
    }

    private String getRedirectUri(HttpServletRequest request) {
        String redirectURI = request.getParameter(ConstantValue.REDIRECT_URI);
        if (redirectURI == null || redirectURI.length() <= 3) {
            log.trace("getRedirectURI - No redirectURI found, setting to {}", "login");
            redirectURI = "login";
        }
        return redirectURI;
    }

}
