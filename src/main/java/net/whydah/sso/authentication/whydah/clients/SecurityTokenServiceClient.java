package net.whydah.sso.authentication.whydah.clients;

import static com.sun.jersey.api.client.ClientResponse.Status.FORBIDDEN;
import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.restfb.types.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.authentication.facebook.FacebookHelper;
import net.whydah.sso.authentication.netiq.NetIQHelper;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.SessionHelper;
import net.whydah.sso.session.baseclasses.BaseSecurityTokenServiceClient;

public class SecurityTokenServiceClient extends BaseSecurityTokenServiceClient {
	   
	 private final Client tokenServiceClient = Client.create();
	 
    public SecurityTokenServiceClient() throws IOException {
    	super(AppConfig.readProperties());
    }
    
    public String getUserToken(UserCredential user, String userticket) {
        if (ApplicationMode.DEV.equals(ApplicationMode.getApplicationMode())){
            return getDummyToken();
        }
        if (true) {  // Command replacement
            String userToken = new CommandLogonUserByUserCredential(uri_securitytoken_service, getMyAppTokenID(), getMyAppTokenXml(), user.toXML(), userticket).execute();
            log.debug("getUserToken - Log on returned with userToken {}", userToken);
            if (userToken == null || userToken.length() < 7) {
                return null;
            }
            return userToken;
        }
        WebResource getUserToken = tokenServiceClient.resource(uri_securitytoken_service).path("user/" + getMyAppTokenID() + "/" + userticket + "/clients");
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("apptoken", getMyAppTokenXml());
        formData.add("usercredential", user.toXML());
        ClientResponse response = getUserToken.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
            log.info("getUserToken - User authentication failed with status code " + response.getStatus());
            return null;
            //throw new IllegalArgumentException("Log on failed. " + ClientResponse.Status.FORBIDDEN);
        }
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("getUserToken - Log on OK with response {}", responseXML);
            SessionHelper.updateApplinks(uri_useradmin_service, getMyAppTokenID(), responseXML);
            return responseXML;
        }

        //retry once for other statuses
        response = getUserToken.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("getUserToken - Log on OK with response {}", responseXML);
            return responseXML;
        } else if (response.getStatus() == NOT_FOUND.getStatusCode()) {
            log.error(printableUrlErrorMessage("getUserToken - Auth failed - Problems connecting with TokenService", getUserToken, response));
        } else {
            log.info(printableUrlErrorMessage("getUserToken - User authentication failed", getUserToken, response));
        }
        return null;
        //throw new RuntimeException("User authentication failed with status code " + response.getStatus());
    }

    
    public String createAndLogonUser(User fbUser, String fbAccessToken, UserCredential userCredential, String userticket) {
    	
        log.debug("apptokenid: {}", getMyAppTokenID());


        WebResource createUserResource = tokenServiceClient.resource(uri_securitytoken_service).path("user/" + getMyAppTokenID() + "/" + userticket + "/create_user");
        log.trace("createUserResource:"+createUserResource.toString());

        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("apptoken", getMyAppTokenXml());
        formData.add("usercredential", userCredential.toXML());
        String facebookUserAsXml = FacebookHelper.getFacebookUserAsXml(fbUser, fbAccessToken);
        formData.add("fbuser", facebookUserAsXml);
        log.trace("createAndLogonUser with fbuser XML: " + facebookUserAsXml+"\nformData:\n"+formData);
        log.info("createAndLogonUser username=" + fbUser.getUsername());
        ClientResponse response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

        //No need to retry if we know it is forbidden.
        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
            //throw new IllegalArgumentException("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
            log.warn("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
            return null;
        }
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("createAndLogonUser OK with response {}", responseXML);
            SessionHelper.updateApplinks(uri_useradmin_service, getMyAppTokenID(), responseXML);
            return responseXML;
        }

        //retry once for other statuses
        response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("createAndLogonUser OK with response {}", responseXML);
            return responseXML;
        }

        log.warn(printableUrlErrorMessage("createAndLogonUser failed after retrying once.", createUserResource, response));
        return null;
        //throw new RuntimeException("createAndLogonUser failed with status code " + response.getStatus());
    }
    
    public String createAndLogonUser(String netiqUserName, String netiqAccessToken, UserCredential userCredential, String userticket,HttpServletRequest request) {
        log.debug("createAndLogonUser - apptokenid: {}", getMyAppTokenID());

        WebResource createUserResource = tokenServiceClient.resource(uri_securitytoken_service).path("user/" + getMyAppTokenID() + "/" + userticket + "/create_user");
        log.debug("createUserResource:"+createUserResource.toString());


        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("apptoken", getMyAppTokenXml());
        formData.add("usercredential", userCredential.toXML());
        NetIQHelper helper = new NetIQHelper();
        String netIQUserAsXml = helper.getNetIQUserAsXml(request);
        formData.add("fbuser", netIQUserAsXml);
        log.trace("createAndLogonUser with netiquser XML: " + netIQUserAsXml+"\nformData:\n"+formData);
        log.info("createAndLogonUser username=" + helper.getUserName(request));
        ClientResponse response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

        //No need to retry if we know it is forbidden.
        if (response.getStatus() == FORBIDDEN.getStatusCode()) {
            //throw new IllegalArgumentException("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
            log.warn("createAndLogonUser failed. username=" + helper.getUserName(request) + ", id=" + helper.getEmail(request));
            return null;
        }
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("createAndLogonUser OK with response {}", responseXML);
            SessionHelper.updateApplinks(uri_useradmin_service, getMyAppTokenID(), responseXML);
            return responseXML;
        }

        //retry once for other statuses
        response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        if (response.getStatus() == OK.getStatusCode()) {
            String responseXML = response.getEntity(String.class);
            log.debug("createAndLogonUser OK with response {}", responseXML);
            return responseXML;
        }

        log.warn("createAndLogonUser failed after retrying once.");
        return null;
        //throw new RuntimeException("createAndLogonUser failed with status code " + response.getStatus());
    }
    
    public static  String printableUrlErrorMessage(String errorMessage, WebResource request, ClientResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(errorMessage);
        sb.append(" Code: ");
        if(response != null) {
            sb.append(response.getStatus());
            sb.append(" URL: ");
        }
        if(request != null) {
            sb.append(request.toString());
        }
        return sb.toString();
    }
}


