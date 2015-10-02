package net.whydah.sso.useradmin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.whydah.sso.authentication.ModelHelper;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.usertoken.TokenServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Properties;

@Controller
public class NewUserController {

    private static final Logger log = LoggerFactory.getLogger(NewUserController.class);
    private static final Client uasClient = Client.create();
    private URI uasServiceUri;

    private final TokenServiceClient tokenServiceClient = new TokenServiceClient();
    String LOGOURL = "/sso/images/site-logo.png";

    public NewUserController() throws IOException {
        Properties properties = AppConfig.readProperties();
        String MY_APP_URI = properties.getProperty("myuri");
        LOGOURL = properties.getProperty("logourl");
        uasServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("useradminservice")).build();

    }

    @RequestMapping("/signup")
    public String signup(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
        log.trace("/signup entry");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String firstName = request.getParameter("firstname");
        String lastName = request.getParameter("lastname");
        String cellPhone = request.getParameter("cellphone");
        if (email != null && username != null) {
            log.info("Requested signup - email: " + email + "  username: " + username + "  firstname: " + firstName + "  lastname: " + lastName + "  cellphone: " + cellPhone + " ");
            String userJson = "{\"username\":\"" + username +
                    "\", \"firstName\":\"" + firstName +
                    "\", \"lastName\":\"" + lastName +
                    "\", \"personRef\":\"\", \"email\":\"" + email +
                    "\", \"cellPhone\":\"" + cellPhone + "\"}";

            ;
            try {
                WebResource uasWR = uasClient.resource(uasServiceUri).path(tokenServiceClient.getMyAppTokenID()).path("signup");//.path("signup");
                log.trace("signup was called. Calling UAS with url " + uasWR.getURI());

                ClientResponse uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, userJson);
                if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                    String error = uasResponse.getEntity(String.class);
                    log.error(error);
                    model.addAttribute("error", "We were unable to create the requested user at this time. Try different data or try again later.");
                }
                //This functionallity is moved to SignupService in UAS
                /*else {
                    uasWR = uasClient.resource(uasServiceUri).path(tokenServiceClient.getMyAppTokenID() + "/auth/password/reset/username/" + username);
                    uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
                    if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                        String error = uasResponse.getEntity(String.class);
                        log.error(error);
                        model.addAttribute("Unable to send user creation mail to user for username=" + username);
                        model.addAttribute("logoURL", LOGOURL);

                        return "newuser";
                    }
                }
                */

            } catch (IllegalStateException ise) {
                log.info(ise.getMessage());
            } catch (RuntimeException e) {
                log.error("Unkonwn error.", e);
            }

        }

        model.addAttribute("logoURL", LOGOURL);
        return "newuser";
    }

    @RequestMapping("/createnewuser")
    public String createNewUser(HttpServletRequest request, HttpServletResponse response, Model model) throws MalformedURLException {
        log.trace("/createnewuser entry");
        model.addAttribute("logoURL", LOGOURL);
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


        String userTokenXml = tokenServiceClient.createAndLogonUser(null, "", userCredential, "");
        if (userTokenXml == null) {
            log.error("createAndLogonUser failed. Redirecting to login page.");
            String redirectURI = "";
            model.addAttribute("redirectURI", redirectURI);
            model.addAttribute("loginError", "Login error: Could not create or authenticate user.");
            ModelHelper.setEnabledLoginTypes(model);
            return "login";
        }
        String clientRedirectURI = request.getParameter("redirectURI");
        model.addAttribute("logoURL", LOGOURL);

        if (clientRedirectURI == null || clientRedirectURI.length() < 3) {
            model.addAttribute("redirect", "welcome");
        } else {
            model.addAttribute("redirect", clientRedirectURI);
        }
        return "action";
    }


}
