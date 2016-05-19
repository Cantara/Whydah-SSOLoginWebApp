package net.whydah.sso.authentication.netiq;

import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.config.SessionHelper;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.authentication.whydah.clients.WhyDahServiceClient;
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
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Controller
public class NetIQLoginController {
        private static final Logger log = LoggerFactory.getLogger(NetIQLoginController.class);
    private final WhyDahServiceClient tokenServiceClient = new WhyDahServiceClient();

        // set this to your servlet URL for the authentication servlet/filter
        private final String hetIQauthURI;
        String LOGOURL="/sso/images/site-logo.png";

        public NetIQLoginController() throws IOException {
            Properties properties = AppConfig.readProperties();
            String MY_APP_URI = properties.getProperty("myuri");
            hetIQauthURI =  properties.getProperty("netIQauthURL");
            LOGOURL=properties.getProperty("logourl");
        }


        @RequestMapping("/netiqlogin")
        public String netIQLogin(HttpServletRequest request, Model model) throws MalformedURLException {
            if (!ModelHelper.isEnabled(ModelHelper.NETIQLOGINENABLED)) {
                return "login";
            }

            String clientRedirectURI = request.getParameter("redirectURI");
            model.addAttribute("logoURL", LOGOURL);

            model.addAttribute("redirect", hetIQauthURI+"?redirectURI="+clientRedirectURI);
            log.info("Redirecting to {}", hetIQauthURI+"?redirectURI="+clientRedirectURI);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "action";
        }

        @RequestMapping("/netiqauth")
        public String netiqAuth(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
            if (!ModelHelper.isEnabled(ModelHelper.NETIQLOGINENABLED)) {
                return "login";
            }
            ModelHelper.setEnabledLoginTypes(model);

            model.addAttribute("logoURL", LOGOURL);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());


            try {
                model.addAttribute("netIQtext", AppConfig.readProperties().getProperty("logintype.netiq.text"));
                model.addAttribute("netIQimage", AppConfig.readProperties().getProperty("logintype.netiq.logo"));
            } catch (IOException ioe) {
                model.addAttribute("netIQtext", "NetIQ");
                model.addAttribute("netIQimage", "images/netiqlogo.png");
            }
            Enumeration headerNames = request.getHeaderNames();
            while(headerNames.hasMoreElements()) {
                String headerName = (String)headerNames.nextElement();
                log.trace("HTTP header - Name:{}  Header: {}",headerName,request.getHeader(headerName));
                if (!NetIQHelper.verifyNetIQHeader(headerName,request.getHeader(headerName))){
                    model.addAttribute("loginError", "Could not log in because NetIQ redirect verification failure.");

                    return "login";
                }
            }
            NetIQHelper helper = new NetIQHelper();
            log.info(helper.getNetIQUserAsXml(request));
            Map.Entry<String, String> pair = helper.findNetIQUserFromRequest(request);
            if (pair == null) {
                log.error("Could not find NetIQ user.");
                //TODO Do we need to add client redirect URI here?
                model.addAttribute("loginError", "Could not find NetIQ user.");
                return "login";
            }
            String netiqAccessToken = pair.getKey();
            String netIQUser = pair.getValue();

            UserCredential userCredential;
            try {
                userCredential = new NetIQUserCredential(netiqAccessToken, netIQUser);
            } catch(IllegalArgumentException iae) {
                log.error(iae.getLocalizedMessage());
                //TODO Do we need to add client redirect URI here?
                model.addAttribute("loginError", "Illegal userdata from netIQ.");
                return "login";
            }

            String ticket = UUID.randomUUID().toString();

            //Check om fbToken har session i lokal cache i TokenService
            // Hvis ja, hent whydah user clients og legg ticket på model eller på returURL.
            String userTokenXml = tokenServiceClient.getUserToken(userCredential, ticket);

            log.debug("NetIQ respsonse:" + userTokenXml);
            if (userTokenXml == null) {
                log.info("getUserToken failed. Try to create new user using netiq credentials.");
                // Hvis nei, hent brukerinfo fra FB, kall tokenService. med user credentials for ny bruker (lag tjenesten i TokenService).
                // Success etter ny bruker er laget = clients. Alltid ticket id som skal sendes.


                userTokenXml = tokenServiceClient.createAndLogonUser(netIQUser, netiqAccessToken, userCredential, ticket,request);
                if (userTokenXml == null) {
                    log.error("createAndLogonUser failed. Redirecting to login page.");
                    String redirectURI = request.getParameter("redirectURI");
                    model.addAttribute("redirectURI", redirectURI);
                    model.addAttribute("loginError", "Login error: Could not create or authenticate user.");
                    ModelHelper.setEnabledLoginTypes(model);
                    return "login";
                }
            }


            String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
            Integer tokenRemainingLifetimeSeconds = WhyDahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
            CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

            String clientRedirectURI = request.getParameter("redirectURI");
            if (clientRedirectURI!=null) {
                clientRedirectURI = tokenServiceClient.appendTicketToRedirectURI(clientRedirectURI, ticket);
                log.info("Redirecting to {}", clientRedirectURI);
                model.addAttribute("redirect", clientRedirectURI);
            }
            return "action";
        }

    }