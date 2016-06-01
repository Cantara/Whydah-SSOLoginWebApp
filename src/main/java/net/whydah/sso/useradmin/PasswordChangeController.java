package net.whydah.sso.useradmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

/**
 * Password management self service.
 */
@Controller
public class PasswordChangeController {
    private static final Logger log = LoggerFactory.getLogger(PasswordChangeController.class);
    //static final MetricRegistry metrics = new MetricRegistry();
    //private final Meter resetPasswordRequests = metrics.meter("requests");
    private static final Client uasClient = Client.create();
    private URI uasServiceUri;


    public PasswordChangeController() throws IOException {
        uasServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("useradminservice")).build();
    }

    @RequestMapping("/test/*")
    public String t1(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @RequestMapping("/resetpassword")
    public String resetpassword(HttpServletRequest request, Model model) {
        log.trace("resetpassword was called");
        SessionDao.instance.addModel_LOGO_URL(model);
        SessionDao.instance.addModel_MYURI(model);
        
        String username = sanitizeUsername(request.getParameter("username"));
        if (username == null) {
            return "resetpassword";
        }

        WebResource uasWR = uasClient.resource(uasServiceUri).path(SessionDao.instance.getServiceClient().getMyAppTokenID()+"/auth/password/reset/username/" + username);
        ClientResponse response = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String error = response.getEntity(String.class);
            log.error(error);
            model.addAttribute("error", error+"\nusername:"+username);
            return "resetpassword";
        }
        return "resetpassworddone";
    }

    @RequestMapping("/changepassword/*")
    public String changePasswordFromLink(HttpServletRequest request, Model model) {
        log.warn("changePasswordFromLink was called");
        PasswordChangeToken passwordChangeToken = getTokenFromPath(request);
      //model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        SessionDao.instance.addModel_LOGO_URL(model);
        //model.addAttribute(ConstantValue.MYURI, properties.getProperty("myuri"));
        SessionDao.instance.addModel_MYURI(model);
        model.addAttribute("username", passwordChangeToken.getUser());
        model.addAttribute("token", passwordChangeToken.getToken());
        if (!passwordChangeToken.isValid()) {
            return "changepasswordtokentimedout";
        } else {
            return "changepassword";
        }
    }

    @RequestMapping("/dochangepassword/*")
    public String doChangePasswordFromLink(HttpServletRequest request, Model model) {
        log.trace("doChangePasswordFromLink was called");
        SessionDao.instance.addModel_LOGO_URL(model);
        
        PasswordChangeToken passwordChangeToken = getTokenFromPath(request);
        String newpassword = request.getParameter("newpassword");
//        WebResource uibWR = uibClient.resource(uibServiceUri).path("/password/" + tokenServiceClient.getMyAppTokenID() + "/reset/username/" + passwordChangeToken.getUser() + "/newpassword/" + passwordChangeToken.getToken());
        WebResource uasWR = uasClient.resource(uasServiceUri).path(SessionDao.instance.getServiceClient().getMyAppTokenID() + "/auth/password/reset/username/" + passwordChangeToken.getUser() + "/newpassword/" + passwordChangeToken.getToken());
        log.trace("doChangePasswordFromLink was called. Calling UAS with url " + uasWR.getURI());

        ClientResponse response = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, "{\"newpassword\":\"" + newpassword + "\"}");
        model.addAttribute("username", passwordChangeToken.getUser());
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String error = response.getEntity(String.class);
            log.error(error);
            if (response.getStatus() == ClientResponse.Status.NOT_ACCEPTABLE.getStatusCode()) {
                model.addAttribute("error", "The password you entered was found to be too weak, please try another password.");
            } else {
                model.addAttribute("error", error + "\nusername:" + passwordChangeToken.getUser());
            }
            model.addAttribute("token", passwordChangeToken.getToken());
            return "changepassword";
        }
        String redirectURI = "/sso/welcome";
        model.addAttribute(ConstantValue.REDIRECT, redirectURI);
        log.info("login - Redirecting to {}", redirectURI);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_CSRFtoken(model);
        return "action";
    }

    private PasswordChangeToken getTokenFromPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        String tokenString = path.substring(path.lastIndexOf('/') + 1);
        return new PasswordChangeToken(tokenString);
    }

    public static String sanitize(String string) {
        if (string == null || string.length() < 3) {
            return string;
        }
        return string
                .replaceAll("(?i)%3c%2fnoscript%3e", "")   // case 1
                .replaceAll("(?i)%2fscript%3e", "")   // case 1
                .replaceAll("(?i)<script.*?>.*?</script.*?>", "")   // case 1
                .replaceAll("(?i)<.*?javascript:.*?>.*?</.*?>", "") // case 2
                .replaceAll("(?i)<.*?\\s+on.*?>.*?</.*?>", "");     // case 3
    }

    public static String sanitizeUsername(String string) {
        if (string == null || string.length() < 3) {
            return string;
        }
        return string
                .replaceAll("(?i)%3c%2fnoscript%3e", "")   // case 1
                .replaceAll("(?i)%2fscript%3e", "")   // case 1
                .replaceAll("(?i)<script.*?>.*?</script.*?>", "")   // case 1
                .replaceAll("(?i)<.*?javascript:.*?>.*?</.*?>", "") // case 2
                .replaceAll("(?i)<.*?script:.*?>.*?</.*?>", "") // case 2
                .replaceAll("(?i)<.*?\\s+on.*?>.*?</.*?>", "");     // case 3
    }

}
