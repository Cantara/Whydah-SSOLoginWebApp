package net.whydah.sso.useradmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.commands.adminapi.user.CommandAddUser;
import net.whydah.sso.commands.adminapi.user.CommandResetUserPassword;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Enumeration;

@Controller
public class NewUserController {

    private static final Logger log = LoggerFactory.getLogger(NewUserController.class);
    private static final Client uasClient = Client.create();
    private URI uasServiceUri;

    //private final WhydahServiceClient tokenServiceClient = new WhydahServiceClient();
    //String LOGOURL = "/sso/images/site-logo.png";

    public NewUserController() throws IOException {
//        Properties properties = AppConfig.readProperties();
//        String MY_APP_URI = properties.getProperty("myuri");
//        LOGOURL = properties.getProperty("logourl");
        uasServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("useradminservice")).build();

    }

    @RequestMapping("/signup")
    public String signup(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
        if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.SIGNUPENABLED)) {
            return "login";
        }
        log.trace("/signup entry");
        printParams(request);
        //model.addAttribute("logoURL", LOGOURL);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);

        SessionDao.instance.addModel_LOGO_URL(model);
        String username = request.getParameter("username");
        String email = request.getParameter("useremail");
        String firstName = request.getParameter("firstname");
        String lastName = request.getParameter("lastname");
        String cellPhone = request.getParameter("cellphone");
        log.trace("signup requested user - email: {} and username: {}", email, username);
        if (email != null && username != null) {
            log.info("Requested signup - email: " + email + "  username: " + username + "  firstname: " + firstName + "  lastname: " + lastName + "  cellphone: " + cellPhone + " ");
            String userJson = "{\"username\":\"" + username +
                    "\", \"firstName\":\"" + firstName +
                    "\", \"lastName\":\"" + lastName +
                    "\", \"personRef\":\"\", \"email\":\"" + email +
                    "\", \"cellPhone\":\"" + cellPhone + "\"}";
            UserIdentity signupUser = UserIdentityMapper.fromUserIdentityWithNoIdentityJson(userJson);
            if (!SessionDao.instance.validCSRFToken(SessionDao.instance.getfromRequest_CSRFtoken(request))) {
                log.warn("action - CSRFtoken verification failed. Redirecting to login.");
                //ModelHelper.setEnabledLoginTypes(model);
                SessionDao.instance.addModel_LoginTypes(model);
                SessionDao.instance.addModel_CSRFtoken(model);
                return "login";
            }

            try {
                WebResource uasWR = uasClient.resource(uasServiceUri).path(SessionDao.instance.getServiceClient().getMyAppTokenID()).path("signup");//.path("signup");
                log.debug("signup was called. Calling UAS with url " + uasWR.getURI());

                // Move to new hystrix userAdd command
                String userAddRoleResult = new CommandAddUser(uasServiceUri, SessionDao.instance.getServiceClient().getMyAppTokenID(), SessionDao.instance.getUserAdminToken().getUserTokenId(), userJson).execute();

                if (userAddRoleResult != null && userAddRoleResult.length() > 4) {
                    new CommandResetUserPassword(uasServiceUri, SessionDao.instance.getServiceClient().getMyAppTokenID(), username).execute();
                    model.addAttribute("username", username);
                    SessionDao.instance.addModel_LoginTypes(model);
                    SessionDao.instance.addModel_CSRFtoken(model);
                    return "signup_result";

                }

                ClientResponse uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, userJson);
                if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                    String error = uasResponse.getEntity(String.class);
                    log.error("error:{} \n URL: {} \n user:{}", error, uasWR.getURI().toString(), signupUser);
                    model.addAttribute("error", "We were unable to create the requested user at this time. Try different data or try again later.");
                } else {
                   // ModelHelper.setEnabledLoginTypes(model);
                    //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                    model.addAttribute("username", username);
                    SessionDao.instance.addModel_LoginTypes(model);
                    SessionDao.instance.addModel_CSRFtoken(model);
                    return "signup_result";
                }

            } catch (IllegalStateException ise) {
                log.info("IllegalStateException {}", ise);
            } catch (RuntimeException e) {
                log.error("Unkonwn error.", e);
            }

        }

        //ModelHelper.setEnabledLoginTypes(model);
        SessionDao.instance.addModel_LoginTypes(model);
        CookieManager.clearUserTokenCookies(request, response);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_CSRFtoken(model);
        return "newuser";
    }

    @RequestMapping("/createnewuser")
    public String createNewUser(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
        if (!SessionDao.instance.isLoginTypeEnabled(ConstantValue.SIGNUPENABLED)) {
            return "login";
        }
        log.trace("/createnewuser entry");
        //model.addAttribute("logoURL", LOGOURL);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_LOGO_URL(model);
        SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);


        //String fbId = "";
        //String username = "user";
        UserCredential userCredential = new UserCredential() {
            @Override
            public String toXML() {
                return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                        "<usercredential>\n" +
                        "    <params>\n" +
                        "        <fbId>" + "" + "</fbId>\n" +
                        "        <username>" + "user" + "</username>\n" +
                        "    </params> \n" +
                        "</usercredential>\n";
            }
        };


        String userTokenXml = SessionDao.instance.getServiceClient().createAndLogonUser(null, "", userCredential, "");
        if (userTokenXml == null) {
            log.error("createAndLogonUser failed. Redirecting to login page.");
            String redirectURI = "";
            model.addAttribute("redirectURI", redirectURI);
            model.addAttribute("loginError", "Login error: Could not create or authenticate user.");
            //ModelHelper.setEnabledLoginTypes(model);
            SessionDao.instance.addModel_LoginTypes(model);
            return "login";
        }
        String clientRedirectURI = request.getParameter("redirectURI");
        //model.addAttribute("logoURL", LOGOURL);
        SessionDao.instance.addModel_LOGO_URL(model);

        if (clientRedirectURI == null || clientRedirectURI.length() < 3) {
            model.addAttribute("redirect", "welcome");
        } else {
            model.addAttribute("redirect", clientRedirectURI);
        }
        return "action";
    }

    private void printParams(HttpServletRequest req) {
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String logString = "";
            String paramName = parameterNames.nextElement();
            logString = logString + paramName + " - ";
            String[] paramValues = req.getParameterValues(paramName);
            for (int i = 0; i < paramValues.length; i++) {
                String paramValue = paramValues[i];
                logString = logString + paramValue;
            }
            log.trace("signup - request params: " + logString);
        }
    }

}
