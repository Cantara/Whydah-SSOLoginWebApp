//package net.whydah.sso.authentication.whydah.clients;
//
//import com.restfb.types.User;
//import com.sun.jersey.api.client.Client;
//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;
//import com.sun.jersey.core.util.MultivaluedMapImpl;
//import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
//import net.whydah.sso.config.ModelHelper;
//import net.whydah.sso.config.SessionHelper;
//import net.whydah.sso.session.WhydahApplicationSession;
//import net.whydah.sso.user.helpers.UserHelper;
//import net.whydah.sso.authentication.UserCredential;
//import net.whydah.sso.authentication.facebook.FacebookHelper;
//import net.whydah.sso.authentication.netiq.NetIQHelper;
//import net.whydah.sso.user.helpers.UserTokenXpathHelper;
//import net.whydah.sso.config.AppConfig;
//import net.whydah.sso.config.ApplicationMode;
//import net.whydah.sso.util.SSLTool;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.MultivaluedMap;
//import javax.ws.rs.core.UriBuilder;
//import java.io.IOException;
//import java.net.URI;
//import java.util.Properties;
//
//import static com.sun.jersey.api.client.ClientResponse.Status.*;
//
//public class SecurityTokenServiceClient_old {
//
//    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceClient_old.class);
//
//    private final URI tokenServiceUri;
//    private final URI userAdminServiceUri;
//    private final Client tokenServiceClient = Client.create();
//    private static WhydahApplicationSession was = null;
//
//    public SecurityTokenServiceClient_old() {
//
//        try {
//            Properties properties = AppConfig.readProperties();
//            // Property-overwrite of SSL verification to support weak ssl certificates
//            if ("disabled".equalsIgnoreCase(properties.getProperty("sslverification"))) {
//                SSLTool.disableCertificateValidation();
//            }
//            this.tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
//            this.userAdminServiceUri = UriBuilder.fromUri(properties.getProperty("useradminservice")).build();
//            String applicationid = properties.getProperty("applicationid");
//            String applicationname = properties.getProperty("applicationname");
//            String applicationsecret = properties.getProperty("applicationsecret");
//
//            if (was == null) {
//                was = WhydahApplicationSession.getInstance(properties.getProperty("securitytokenservice"), applicationid, applicationname, applicationsecret);
//            }
//        } catch (IOException e) {
//            throw new IllegalArgumentException("Error constructing SSOHelper.", e);
//        }
//    }
//
//    public String getUserToken(UserCredential user, String userticket) {
//        if (ApplicationMode.DEV.equals(ApplicationMode.getApplicationMode())){
//            return getDummyToken();
//        }
//        if (true) {  // Command replacement
//            String userToken = new CommandLogonUserByUserCredential(tokenServiceUri, was.getActiveApplicationTokenId(), was.getActiveApplicationTokenXML(), user.toXML(), userticket).execute();
//            log.debug("getUserToken - Log on returned with userToken {}", userToken);
//            if (userToken == null || userToken.length() < 7) {
//                return null;
//            }
//            return userToken;
//        }
//        WebResource getUserToken = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/" + userticket + "/clients");
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("usercredential", user.toXML());
//        ClientResponse response = getUserToken.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            log.info("getUserToken - User authentication failed with status code " + response.getStatus());
//            return null;
//            //throw new IllegalArgumentException("Log on failed. " + ClientResponse.Status.FORBIDDEN);
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("getUserToken - Log on OK with response {}", responseXML);
//            SessionHelper.updateApplinks(userAdminServiceUri, was.getActiveApplicationTokenId(), responseXML);
//            return responseXML;
//        }
//
//        //retry once for other statuses
//        response = getUserToken.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("getUserToken - Log on OK with response {}", responseXML);
//            return responseXML;
//        } else if (response.getStatus() == NOT_FOUND.getStatusCode()) {
//            log.error(printableUrlErrorMessage("getUserToken - Auth failed - Problems connecting with TokenService", getUserToken, response));
//        } else {
//            log.info(printableUrlErrorMessage("getUserToken - User authentication failed", getUserToken, response));
//        }
//        return null;
//        //throw new RuntimeException("User authentication failed with status code " + response.getStatus());
//    }
//
//
//    public boolean createTicketForUserTokenID(String userticket, String userTokenID){
//        log.debug("createTicketForUserTokenID - apptokenid: {}", was.getActiveApplicationTokenId());
//        log.debug("createTicketForUserTokenID - userticket: {} userTokenID: {}", userticket, userTokenID);
//
//        WebResource getUserToken = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/create_userticket_by_usertokenid");
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("userticket", userticket);
//        formData.add("usertokenid", userTokenID);
//        ClientResponse response = getUserToken.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            log.info("createTicketForUserTokenID - failed with status code " + response.getStatus());
//            //throw new IllegalArgumentException("Log on failed. " + ClientResponse.Status.FORBIDDEN);
//            return false;
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("createTicketForUserTokenID - OK with response {}", responseXML);
//            return true;
//        }
//        log.warn("createTicketForUserTokenID - unable to create ticket for usertokenid. Response={}",response.getStatus());
//        return false;
//
//    }
//    public String getUserTokenByUserTicket(String userticket) {
//        if (ApplicationMode.DEV.equals(ApplicationMode.getApplicationMode())){
//            return getDummyToken();
//        }
//
//        WebResource userTokenResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/get_usertoken_by_userticket");
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        log.trace("getUserTokenByUserTicket - ticket: {} apptoken: {}", userticket, was.getActiveApplicationTokenXML());
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("userticket", userticket);
//        ClientResponse response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            log.warn("getUserTokenByUserTicket failed");
//            throw new IllegalArgumentException("getUserTokenByUserTicket failed.");
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("Response OK with XML: {}", responseXML);
//            SessionHelper.updateApplinks(userAdminServiceUri, was.getActiveApplicationTokenId(), responseXML);
//            return responseXML;
//        }
//        //retry
//        response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("Response OK with XML: {}", responseXML);
//            return responseXML;
//        }
//        String authenticationFailedMessage = printableUrlErrorMessage("User authentication failed", userTokenResource, response);
//        log.warn(authenticationFailedMessage);
//        throw new RuntimeException(authenticationFailedMessage);
//    }
//
//    public String getUserTokenByUserTokenID(String usertokenId) {
//        if (ApplicationMode.DEV.equals(ApplicationMode.getApplicationMode())) {
//            return getDummyToken();
//        }
//
//        WebResource userTokenResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/get_usertoken_by_usertokenid");
//        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("usertokenid", usertokenId);
//        ClientResponse response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            throw new IllegalArgumentException("Login failed.");
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("Response OK with XML: {}", responseXML);
//            SessionHelper.updateApplinks(userAdminServiceUri, was.getActiveApplicationTokenId(), responseXML);
//            return responseXML;
//        }
//        //retry
//        response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("Response OK with XML: {}", responseXML);
//            return responseXML;
//        }
//        String authenticationFailedMessage = printableUrlErrorMessage("User authentication failed", userTokenResource, response);
//        log.warn(authenticationFailedMessage);
//        throw new RuntimeException(authenticationFailedMessage);
//    }
//
//    public void releaseUserToken(String userTokenId) {
//        log.trace("Releasing userTokenId={}", userTokenId);
//        WebResource releaseResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/release_usertoken");
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        formData.add(ModelHelper.USER_TOKEN_ID, userTokenId);
//        ClientResponse response = releaseResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() != OK.getStatusCode()) {
//            log.warn("releaseUserToken failed for userTokenId={}: {}", userTokenId, response);
//        }
//        log.trace("Released userTokenId={}", userTokenId);
//    }
//
//    public boolean verifyUserTokenId(String usertokenid) {
//        // If we get strange values...  return false
//        if (usertokenid == null || usertokenid.length() < 4) {
//            log.trace("verifyUserTokenId - Called with bogus usertokenid={}. return false",usertokenid);
//            return false;
//        }
//        WebResource verifyResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/validate_usertokenid/" + usertokenid);
//        ClientResponse response = verifyResource.get(ClientResponse.class);
//        if (response.getStatus() == OK.getStatusCode()) {
//            log.debug("verifyUserTokenId - usertokenid validated OK");
//            return true;
//        }
//        if(response.getStatus() == CONFLICT.getStatusCode()) {
//            log.debug("verifyUserTokenId - usertokenid not ok: {}", response);
//            return false;
//        }
//        //retry
//        log.info("verifyUserTokenId - retrying usertokenid ");
//        response = verifyResource.get(ClientResponse.class);
//        boolean bolRes = response.getStatus() == OK.getStatusCode();
//        log.debug("verifyUserTokenId - validate_usertokenid {}  result {}", "user/" + was.getActiveApplicationTokenId() + "/validate_usertokenid/" + usertokenid, response);
//        return bolRes;
//    }
//
//    public String createAndLogonUser(User fbUser, String fbAccessToken, UserCredential userCredential, String userticket) {
//        log.debug("apptokenid: {}", was.getActiveApplicationTokenId());
//
//
//        WebResource createUserResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/" + userticket + "/create_user");
//        log.trace("createUserResource:"+createUserResource.toString());
//
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("usercredential", userCredential.toXML());
//        String facebookUserAsXml = FacebookHelper.getFacebookUserAsXml(fbUser, fbAccessToken);
//        formData.add("fbuser", facebookUserAsXml);
//        log.trace("createAndLogonUser with fbuser XML: " + facebookUserAsXml+"\nformData:\n"+formData);
//        log.info("createAndLogonUser username=" + fbUser.getUsername());
//        ClientResponse response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//
//        //No need to retry if we know it is forbidden.
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            //throw new IllegalArgumentException("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
//            log.warn("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
//            return null;
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("createAndLogonUser OK with response {}", responseXML);
//            SessionHelper.updateApplinks(userAdminServiceUri, was.getActiveApplicationTokenId(), responseXML);
//            return responseXML;
//        }
//
//        //retry once for other statuses
//        response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("createAndLogonUser OK with response {}", responseXML);
//            return responseXML;
//        }
//
//        log.warn(printableUrlErrorMessage("createAndLogonUser failed after retrying once.", createUserResource, response));
//        return null;
//        //throw new RuntimeException("createAndLogonUser failed with status code " + response.getStatus());
//    }
//
//    public String createAndLogonUser(String netiqUserName, String netiqAccessToken, UserCredential userCredential, String userticket,HttpServletRequest request) {
//        log.debug("createAndLogonUser - apptokenid: {}", was.getActiveApplicationTokenId());
//
//        WebResource createUserResource = tokenServiceClient.resource(tokenServiceUri).path("user/" + was.getActiveApplicationTokenId() + "/" + userticket + "/create_user");
//        log.debug("createUserResource:"+createUserResource.toString());
//
//
//        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
//        formData.add("apptoken", was.getActiveApplicationTokenXML());
//        formData.add("usercredential", userCredential.toXML());
//        NetIQHelper helper = new NetIQHelper();
//        String netIQUserAsXml = helper.getNetIQUserAsXml(request);
//        formData.add("fbuser", netIQUserAsXml);
//        log.trace("createAndLogonUser with netiquser XML: " + netIQUserAsXml+"\nformData:\n"+formData);
//        log.info("createAndLogonUser username=" + helper.getUserName(request));
//        ClientResponse response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//
//        //No need to retry if we know it is forbidden.
//        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
//            //throw new IllegalArgumentException("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
//            log.warn("createAndLogonUser failed. username=" + helper.getUserName(request) + ", id=" + helper.getEmail(request));
//            return null;
//        }
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("createAndLogonUser OK with response {}", responseXML);
//            SessionHelper.updateApplinks(userAdminServiceUri, was.getActiveApplicationTokenId(), responseXML);
//            return responseXML;
//        }
//
//        //retry once for other statuses
//        response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
//        if (response.getStatus() == OK.getStatusCode()) {
//            String responseXML = response.getEntity(String.class);
//            log.debug("createAndLogonUser OK with response {}", responseXML);
//            return responseXML;
//        }
//
//        log.warn("createAndLogonUser failed after retrying once.");
//        return null;
//        //throw new RuntimeException("createAndLogonUser failed with status code " + response.getStatus());
//    }
//
//
//    public static  String getDummyToken(){
//        return UserHelper.getDummyUserToken();
//    }
//
//    public static  String printableUrlErrorMessage(String errorMessage, WebResource request, ClientResponse response) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(errorMessage);
//        sb.append(" Code: ");
//        if(response != null) {
//            sb.append(response.getStatus());
//            sb.append(" URL: ");
//        }
//        if(request != null) {
//            sb.append(request.toString());
//        }
//        return sb.toString();
//    }
//
//
//    public static String getMyAppTokenID() {
//        return was.getActiveApplicationTokenId();
//    }
//    public static String getMyAppTokenXml() {
//        return was.getActiveApplicationTokenXML();
//    }
//
//    public static Integer calculateTokenRemainingLifetimeInSeconds(String userTokenXml) {
//        Integer tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
//        Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);
//
//        if (tokenLifespanMs == null || tokenTimestampMsSinceEpoch == null) {
//            return null;
//        }
//
//        long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;
//        long remainingLifeMs = endOfTokenLifeMs - System.currentTimeMillis();
//        return (int) (remainingLifeMs / 1000);
//    }
//
//
//    public String appendTicketToRedirectURI(String redirectURI, String userticket) {
//        char paramSep = redirectURI.contains("?") ? '&' : '?';
//        redirectURI += paramSep + ModelHelper.USERTICKET + '=' + userticket;
//        return redirectURI;
//    }
//}
//
