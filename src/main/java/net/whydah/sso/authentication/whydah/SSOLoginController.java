package net.whydah.sso.authentication.whydah;

import net.whydah.sso.ServerRunner;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.commands.extensions.statistics.CommandListUserActivities;
import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.SessionHelper;
import net.whydah.sso.extensions.useractivity.helpers.UserActivityHelper;
import net.whydah.sso.authentication.whydah.clients.SecurityTokenServiceClient;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserTokenID;
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

    private final static Logger log = LoggerFactory.getLogger(SSOLoginController.class);
    private final SecurityTokenServiceClient tokenServiceClient;
    private String LOGOURL = "/sso/images/site-logo.png";
    private String whydahVersion = ServerRunner.version;

    private String crmservice;
    private String reportservice;

    //private final int MIN_REDIRECT_SIZE=4;
    //private final ModelHelper modelHelper = new ModelHelper(this);


    public SSOLoginController() throws IOException {
        Properties properties = AppConfig.readProperties();
        LOGOURL = properties.getProperty("logourl");
        crmservice = properties.getProperty("crmservice");
        reportservice = properties.getProperty("reportservice");
        this.tokenServiceClient = new SecurityTokenServiceClient();
    }


    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response,Model model) {
        String redirectURI = SessionHelper.getRedirectURI(request);
        boolean sessionCheckOnly = isSessionCheckOnly(request);
        model.addAttribute(SessionHelper.SESSIONCHECK, sessionCheckOnly);

        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.WHYDAH_VERSION,whydahVersion);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);


        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        CookieManager.addSecurityHTTPHeaders(response);


        //usertokenId = cookieManager.getUserTokenIdFromCookie(request, response);
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        log.trace("login: redirectURI={}, SessionCheck={}, userTokenIdFromCookie={}", redirectURI, sessionCheckOnly, userTokenIdFromCookie);

        UserTokenID whydahUserTokenId = UserTokenID.invalidTokenID();
        if ("logout".equalsIgnoreCase(userTokenIdFromCookie)) {
            log.info("userTokenId={} from cookie. TODO: should probably clear the logout cookie here?", userTokenIdFromCookie);
            CookieManager.clearUserTokenCookies(request, response);
            //usertokenId = WhydahUserTokenId.invalidTokenId();
        } else if (userTokenIdFromCookie != null && tokenServiceClient.verifyUserTokenId(userTokenIdFromCookie)) {
            log.trace("userTokenId={} from cookie verified OK.", userTokenIdFromCookie);
            whydahUserTokenId = UserTokenID.fromUserTokenID(userTokenIdFromCookie);
        } else {
            CookieManager.clearUserTokenCookies(request, response);
        }

        if (whydahUserTokenId.isValid()) {
            log.trace("login - whydahUserTokenId={} is valid", whydahUserTokenId);

            if (SessionHelper.DEFAULT_REDIRECT.equalsIgnoreCase(redirectURI)) {
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

        // Added return is sessioncheck only and no cookie found
        if (isSessionCheckOnly(request)) {
            // Action use redirect - not redirectURI
            model.addAttribute(SessionHelper.REDIRECT, redirectURI);
            log.info("login - isSessionCheckOnly - Redirecting to {}", redirectURI);
            return "action";

        } 

        ModelHelper.setEnabledLoginTypes(model);
        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        return "login";
    }



    @RequestMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response,Model model) {
        String userTicket = request.getParameter(ModelHelper.USERTICKET);
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = "";
        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.IAM_MODE, ApplicationMode.getApplicationMode());
        model.addAttribute(SessionHelper.WHYDAH_VERSION, whydahVersion);
        CookieManager.addSecurityHTTPHeaders(response);
        log.warn("=>>>>>>>>>>>>>>>>>>>> 1 ");
        try {
            if (userTicket != null && userTicket.length() > 3) {
                log.warn("=>>>>>>>>>>>>>>>>>>>> 2 ");
                log.trace("Welcome - Using userTicket");
                userTokenXml = tokenServiceClient.getUserTokenByUserTicket(userTicket);
                log.warn("=>>>>>>>>>>>>>>>>>>>> 3 ");
                model.addAttribute(ModelHelper.USERTICKET, userTicket);
                log.warn("=>>>>>>>>>>>>>>>>>>>> 4 ");
                model.addAttribute(ModelHelper.USERTOKEN, userTokenXml);
                log.warn("=>>>>>>>>>>>>>>>>>>>> 5 ");
            } else if (userTokenId != null && userTokenId.length() > 3) {
                log.warn("=>>>>>>>>>>>>>>>>>>>> 6 ");
                log.trace("Welcome - No userTicket, using userTokenID from cookie");
                userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
                log.warn("=>>>>>>>>>>>>>>>>>>>> 7 ");
                model.addAttribute(ModelHelper.USERTICKET, "No userTicket, using userTokenID");
                log.warn("=>>>>>>>>>>>>>>>>>>>> 8 ");
                model.addAttribute(ModelHelper.USERTOKEN, userTokenXml);
                log.warn("=>>>>>>>>>>>>>>>>>>>> 9 ");
                model.addAttribute(ModelHelper.USER_TOKEN_ID, userTokenId);
            } else {
                // TODO cleanup - this messes up the log for a normal case
                //throw new UnauthorizedException();
                log.trace("Welcome - no session found");
                CookieManager.clearUserTokenCookies(request, response);
                ModelHelper.setEnabledLoginTypes(model);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                return "login";
            }
        } catch (Exception e){
            log.warn("welcome redirect - SecurityTokenException exception: ",e);
            CookieManager.clearUserTokenCookies(request, response);
            ModelHelper.setEnabledLoginTypes(model);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "login";
        }
        log.warn("=>>>>>>>>>>>>>>>>>>>> 11 ");
        model.addAttribute(ModelHelper.USERTOKEN, trim(userTokenXml));
        log.warn("=>>>>>>>>>>>>>>>>>>>> 11 ");
        model.addAttribute(SessionHelper.APP_LINKS, SessionHelper.getAppLinks());
        log.trace("embedded applinks: " + SessionHelper.getAppLinks());
        log.warn("=>>>>>>>>>>>>>>>>>>>> 12 ");
        model.addAttribute(ModelHelper.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
        log.warn("=>>>>>>>>>>>>>>>>>>>> 13 ");
        model.addAttribute(ModelHelper.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
        model.addAttribute(ModelHelper.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
        model.addAttribute(ModelHelper.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
        log.warn("=>>>>>>>>>>>>>>>>>>>> 14 ");
        model.addAttribute(ModelHelper.DEFCON, UserTokenXpathHelper.getDEFCONLevel(userTokenXml));
        model.addAttribute(ModelHelper.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
        log.warn("=>>>>>>>>>>>>>>>>>>>> 15 ");
        addCrmCustomer(model, userTokenXml);
        log.warn("=>>>>>>>>>>>>>>>>>>>> 16 ");
        addUserActivities(model, userTokenXml);
        log.warn("=>>>>>>>>>>>>>>>>>>>> 17 ");
        return "welcome";
    }


    @RequestMapping("/action")
    public String action(HttpServletRequest request, HttpServletResponse response, Model model) {
        String redirectURI = SessionHelper.getRedirectURI(request);
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
            model.addAttribute(ModelHelper.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
            addCrmCustomer(model, userTokenXml);
            addUserActivities(model, userTokenXml);
            return "welcome";
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = SecurityTokenServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
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

    private void addCrmCustomer(Model model, String userToken) {
        if (UserTokenXpathHelper.getPersonref(userToken).length() > 2) {
            try {
                String personRef = net.whydah.sso.user.helpers.UserTokenXpathHelper.getPersonref(userToken);
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userToken);
                String crmCustomerJson = new CommandGetCRMCustomer(URI.create(crmservice), tokenServiceClient.getMyAppTokenID(), userTokenId, personRef).execute();
                model.addAttribute(ModelHelper.CRMCUSTOMER, crmCustomerJson);
                model.addAttribute(ModelHelper.JSON_DATA, crmCustomerJson);
            } catch (Exception e) {

            }
        }
    }

    private void addUserActivities(Model model, String userTokenXml) {
        if (UserTokenXpathHelper.getUserID(userTokenXml).length() > 2) {
            try {
                String userid = UserTokenXpathHelper.getUserID(userTokenXml);
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
                log.warn(">==================== 1 ");

                String userActivitiesJson = new CommandListUserActivities(URI.create(reportservice), SecurityTokenServiceClient.getMyAppTokenID(), userTokenId, userid).execute();
                //    model.addAttribute(ModelHelper.USERACTIVITIES, userActivitiesJson);
                log.warn(">==================== 2 ");
                model.addAttribute(ModelHelper.USERACTIVITIES_SIMPLIFIED, UserActivityHelper.getUserSessionsJsonFromUserActivityJson(userActivitiesJson, userid));
                log.warn(">==================== 3 ");
            } catch (Exception e) {

            }
        }
    }



    private boolean isSessionCheckOnly(HttpServletRequest request) {
        String redirectURI = request.getParameter(SessionHelper.SESSIONCHECK);
        if (redirectURI == null || redirectURI.length() < 1) {
            log.trace("isSessionCheckOnly - false - No SESSIONCHECK param found");
            return false;
        }
        return true;
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


