package net.whydah.sso.authentication.whydah;

import net.whydah.sso.ServerRunner;
import net.whydah.sso.authentication.UnauthorizedException;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.tokenservice.TokenServiceClient;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

@Controller
public class SSOLoginController {
    public static final String DEFAULT_REDIRECT = "welcome";

    private final static Logger log = LoggerFactory.getLogger(SSOLoginController.class);
    private final TokenServiceClient tokenServiceClient;
    private String LOGOURL = "/sso/images/site-logo.png";
    private String whydahVersion = ServerRunner.version;

    //private final int MIN_REDIRECT_SIZE=4;
    //private final ModelHelper modelHelper = new ModelHelper(this);


    public SSOLoginController() throws IOException {
        Properties properties = AppConfig.readProperties();
        LOGOURL = properties.getProperty("logourl");
        this.tokenServiceClient = new TokenServiceClient();
    }


    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response,Model model) {
        String redirectURI = getRedirectURI(request);
        boolean sessionCheckOnly = isSessionCheckOnly(request);

        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.WHYDAH_VERSION,whydahVersion);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);

        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        CookieManager.addSecurityHTTPHeaders(response);


        //usertokenId = cookieManager.getUserTokenIdFromCookie(request, response);
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        log.trace("login: redirectURI={}, SessionCheck={}, userTokenIdFromCookie={}", redirectURI, sessionCheckOnly, userTokenIdFromCookie);

        WhydahUserTokenId whydahUserTokenId = WhydahUserTokenId.invalidTokenId();
        if ("logout".equalsIgnoreCase(userTokenIdFromCookie)) {
            log.info("userTokenId={} from cookie. TODO: should probably clear the logout cookie here?", userTokenIdFromCookie);
            CookieManager.clearUserTokenCookies(request, response);
            //usertokenId = WhydahUserTokenId.invalidTokenId();
        } else if (userTokenIdFromCookie != null && tokenServiceClient.verifyUserTokenId(userTokenIdFromCookie)) {
            log.trace("userTokenId={} from cookie verified OK.", userTokenIdFromCookie);
            whydahUserTokenId = WhydahUserTokenId.fromTokenId(userTokenIdFromCookie);
        } else {
            CookieManager.clearUserTokenCookies(request, response);
        }

        if (whydahUserTokenId.isValid()) {
            log.trace("login - whydahUserTokenId={} is valid", whydahUserTokenId);

            if (DEFAULT_REDIRECT.equalsIgnoreCase(redirectURI)){
                log.trace("login - Did not find any sensible redirectURI, using /welcome");
                model.addAttribute(SessionHelper.REDIRECT, redirectURI);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                log.info("login - Redirecting to {}", redirectURI);
                return "action";

            }
            String userTicket = UUID.randomUUID().toString();
            if (tokenServiceClient.createTicketForUserTokenID(userTicket, whydahUserTokenId.toString())){
                log.info("login - created new userticket={} for usertokenid={}",userTicket, whydahUserTokenId);
                redirectURI = tokenServiceClient.appendTicketToRedirectURI(redirectURI, userTicket);

                // Action use redirect - not redirectURI
                model.addAttribute(SessionHelper.REDIRECT, redirectURI);
                log.info("login - Redirecting to {}", redirectURI);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                return "action";
            }
        }
        ModelHelper.setEnabledLoginTypes(model);
        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        return "login";
    }



    @RequestMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response,Model model) {
        String userTicket = request.getParameter(ModelHelper.USERTICKET);
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userToken;
        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.IAM_MODE, ApplicationMode.getApplicationMode());
        model.addAttribute(SessionHelper.WHYDAH_VERSION, whydahVersion);
        CookieManager.addSecurityHTTPHeaders(response);
        try {
            if (userTicket != null && userTicket.length() > 3) {
                log.trace("Welcome - Using userTicket");
                userToken = tokenServiceClient.getUserTokenByUserTicket(userTicket);
                model.addAttribute(ModelHelper.USERTICKET, userTicket);
                model.addAttribute(ModelHelper.USER_TOKEN_ID, UserTokenXpathHelper.getUserTokenId(userToken));
            } else if (userTokenId != null && userTokenId.length() > 3) {
                log.trace("Welcome - No userTicket, using userTokenID from cookie");
                userToken = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
                model.addAttribute(ModelHelper.USERTICKET, "No userTicket, using userTokenID");
                model.addAttribute(ModelHelper.USER_TOKEN_ID, userTokenId);
            } else {
                // TODO cleanup - this messes up the log for a normal case
                throw new UnauthorizedException();
            }
        } catch (Exception e){
            log.warn("welcome redirect - SecurityTokenException exception: ",e);
            CookieManager.clearUserTokenCookies(request, response);
            ModelHelper.setEnabledLoginTypes(model);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "login";
        }
        model.addAttribute(ModelHelper.USERTOKEN, trim(userToken));
        model.addAttribute(SessionHelper.APP_LINKS, SessionHelper.getAppLinks());
        log.trace("embedded applinks: " + SessionHelper.getAppLinks());
        model.addAttribute(ModelHelper.REALNAME, UserTokenXpathHelper.getRealName(userToken));
        model.addAttribute(ModelHelper.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userToken));
        model.addAttribute(ModelHelper.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userToken));
        model.addAttribute(ModelHelper.EMAIL, UserTokenXpathHelper.getEmail(userToken));
        model.addAttribute(ModelHelper.DEFCON, UserTokenXpathHelper.getDEFCONLevel(userToken));
        return "welcome";
    }

    @RequestMapping("/action")
    public String action(HttpServletRequest request, HttpServletResponse response, Model model) {
        String redirectURI = getRedirectURI(request);
        log.trace("action: redirectURI: {}", redirectURI);
        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.WHYDAH_VERSION,whydahVersion);


        if (!SessionHelper.validCSRFToken(request.getParameter(SessionHelper.CSRFtoken))) {
            log.warn("action - CSRFtoken verification failed. Redirecting to login.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in - CSRFtoken missing or incorrect");
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            ModelHelper.setEnabledLoginTypes(model);
            CookieManager.clearUserTokenCookies(request, response);
            model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
            return "login";

        }
        UserCredential user = new UserNameAndPasswordCredential(request.getParameter(SessionHelper.USER), request.getParameter(SessionHelper.PASSWORD));
        String userTicket = UUID.randomUUID().toString();
        String userTokenXml = tokenServiceClient.getUserToken(user, userTicket);

        if (userTokenXml == null) {
            log.warn("action - getUserToken failed. Redirecting to login.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in.");
            ModelHelper.setEnabledLoginTypes(model);
            CookieManager.clearUserTokenCookies(request, response);
            model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "login";
        }
        if (redirectURI.contains(ModelHelper.USERTICKET)) {
            log.warn("action - redirectURI contain ticket. Redirecting to welcome.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not redirect back, redirect loop detected.");
            ModelHelper.setEnabledLoginTypes(model);
            model.addAttribute(SessionHelper.REDIRECT_URI, "");
            model.addAttribute(ModelHelper.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
            model.addAttribute(ModelHelper.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
            model.addAttribute(ModelHelper.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
            model.addAttribute(ModelHelper.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
            model.addAttribute(ModelHelper.DEFCON, UserTokenXpathHelper.getDEFCONLevel(userTokenXml));
            return "welcome";
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = TokenServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

        // ticket on redirect
        if (redirectURI.toLowerCase().contains(SessionHelper.USERTICKET)) {
            // Do not overwrite ticket
        } else {
            redirectURI = tokenServiceClient.appendTicketToRedirectURI(redirectURI, userTicket);
        }
        // Action use redirect...
        model.addAttribute(SessionHelper.REDIRECT, redirectURI);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        log.info("action - Redirecting to {}", redirectURI);
        return "action";
    }



    private boolean isSessionCheckOnly(HttpServletRequest request) {
        String redirectURI = request.getParameter(SessionHelper.SESSIONCHECK);
        if (redirectURI == null || redirectURI.length() < 1) {
            log.trace("isSessionCheckOnly - false - No SESSIONCHECK param found");
            return false;
        }
        return true;
    }

    private String getRedirectURI(HttpServletRequest request) {
        String redirectURI = request.getParameter(SessionHelper.REDIRECT_URI);
        //log.trace("getRedirectURI - redirectURI from request: {}", redirectURI);
        if (redirectURI == null || redirectURI.length() < 1) {
            log.trace("getRedirectURI - No redirectURI found, setting to {}", DEFAULT_REDIRECT);
            return DEFAULT_REDIRECT;
        }
        try {
            URI redirect = new URI(redirectURI);
            return redirectURI;
        } catch (Exception e){
            return  DEFAULT_REDIRECT;
        }
    }



    public static String trim(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}


