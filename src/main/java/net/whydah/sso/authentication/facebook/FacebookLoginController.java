package net.whydah.sso.authentication.facebook;

import com.restfb.types.User;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.application.RedirectURI;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 26/09/12
 */
@Controller
public class FacebookLoginController {
    private static final Logger log = LoggerFactory.getLogger(FacebookLoginController.class);
    private final WhydahServiceClient tokenServiceClient = new WhydahServiceClient();
    private String LOGOURL = "/sso/images/site-logo.png";


    // set this to your servlet URL for the authentication servlet/filter
    private final String fbauthURI;

    public FacebookLoginController() throws IOException {
        Properties properties = AppConfig.readProperties();
        String MY_APP_URI = properties.getProperty("myuri");
        fbauthURI = MY_APP_URI + "fbauth";
        LOGOURL = properties.getProperty("logourl");
    }


    @RequestMapping("/fblogin")
    public String facebookLogin(HttpServletRequest request, Model model) throws MalformedURLException {
        SessionDao.instance.addModel_CSRFtoken(model);
        model.addAttribute(ConstantValue.LOGO_URL, LOGOURL);
        if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.FACEBOOKLOGIN_ENABLED)) {
            return "login";
        }
        RedirectURI clientRedirectURI = new RedirectURI(request.getParameter("redirectURI"));
        String facebookLoginUrl = FacebookHelper.getFacebookLoginUrl(clientRedirectURI.getInput(), fbauthURI);
        model.addAttribute("redirect", facebookLoginUrl);
        log.info("Redirecting to {}", facebookLoginUrl);
        return "action";
    }

    @RequestMapping("/fbauth")
    public String facebookAuth(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
        SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.addModel_LOGO_URL(model);
        SessionDao.instance.addModel_LoginTypes(model);
        if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.FACEBOOKLOGIN_ENABLED)) {
             return "login";
         }
        String code = request.getParameter("code");
        log.trace("fbauth got code: {}",code);
        log.trace("fbauth - got state: {}",request.getParameter("state"));
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        Map.Entry<String, User> pair = FacebookHelper.loginAndCreateFacebookUser(code, fbauthURI);
        if (pair == null) {
            log.error("Could not fetch facebok user.");
            //TODO Do we need to add client redirect URI here?
           // ModelHelper.setEnabledLoginTypes(model);
            SessionDao.instance.addModel_LoginTypes(model);
            return "login";
        }
        String fbAccessToken = pair.getKey();
        User fbUser = pair.getValue();

        String userticket = UUID.randomUUID().toString();
        UserCredential userCredential;
        try {
            String username = fbUser.getUsername();
            // Since Facebook sometimes returns username=null, we fallback on using facebook user email as username
            if (!UserName.isValid(username)) {
                if (fbUser.getEmail()==null || fbUser.getEmail().length()<6){
                    username=fbUser.getId();
                    log.trace("facebook returned username, email=null, using facebook id as username instead");

                } else {
                    username=fbUser.getEmail();
                    log.trace("facebook returned username=null, using facebook email as username instead");

                }
            }
            log.trace("new FacebookUserCredential(fbUser.getId({}),  getUsername({})",fbUser.getId(), username);
            userCredential = new FacebookUserCredential(fbUser.getId(), username);
        } catch(IllegalArgumentException iae) {
            log.error("fbauth - unable to build usercredential for facebook token.",iae.getLocalizedMessage());
            String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
            model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
            return "login";
        }


        //Check om fbToken har session i lokal cache i TokenService
        // Hvis ja, hent whydah user clients og legg ticket på model eller på returURL.
        String userTokenXml = tokenServiceClient.getUserToken(userCredential, userticket);
        if (userTokenXml == null) {
            log.warn("getUserToken failed. Try to create new user using facebook credentials.");
            // Hvis nei, hent brukerinfo fra FB, kall tokenService. med user credentials for ny bruker (lag tjenesten i TokenService).
            // Success etter ny bruker er laget = clients. Alltid ticket id som skal sendes.


            userTokenXml = tokenServiceClient.createAndLogonUser(fbUser, fbAccessToken, userCredential, userticket);
            if (userTokenXml == null) {
                log.error("createAndLogonUser failed. Did not get a valid UserToken. Redirecting to login page.");
                String redirectURI = getfbauthRedirectURI(request);
                model.addAttribute("redirectURI", redirectURI);
                model.addAttribute("loginError", "Login error: Could not create or authenticate user.");
                //ModelHelper.setEnabledLoginTypes(model);
                SessionDao.instance.addModel_LoginTypes(model);
                //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                SessionDao.instance.addModel_CSRFtoken(model);
                return "login";
            }
        }


        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

        String clientRedirectURI = request.getParameter("state");
        clientRedirectURI = tokenServiceClient.appendTicketToRedirectURI(clientRedirectURI, userticket);
        log.info("Redirecting to {}", clientRedirectURI);
        model.addAttribute("redirect", clientRedirectURI);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_CSRFtoken(model);
        return "action";
    }

    private String getfbauthRedirectURI(HttpServletRequest request) {
        String redirectURI = request.getParameter("fbauthURI");
        log.debug("fbauthURI from request: {}", redirectURI);
        if (redirectURI == null || redirectURI.length() < 4) {
            log.warn("No fbauthURI found, setting to {}", ConstantValue.DEFAULT_REDIRECT);
            return ConstantValue.DEFAULT_REDIRECT;
        }
        return redirectURI;
    }


}