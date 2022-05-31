package net.whydah.sso.authentication.whydah.clients;

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
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.session.baseclasses.BaseDevelopmentWhydahServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Properties;

import static com.sun.jersey.api.client.ClientResponse.Status.*;

public class WhydahServiceClient extends BaseDevelopmentWhydahServiceClient {

    private final static Logger log = LoggerFactory.getLogger(WhydahServiceClient.class);

    //TODO: HUYDO will check, move all general functions to base
	
	private final Client tokenServiceClient = Client.create();

    public WhydahServiceClient() throws IOException {
        super(AppConfig.readProperties());
    }
    
    public WhydahServiceClient(Properties pros) throws IOException {
        super(pros);
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
            SessionDao.instance.updateApplinks();
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
            SessionDao.instance.updateApplinks();
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
            SessionDao.instance.updateApplinks();
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
    
    public String createAndLogonUser(String provider, String accessToken, String appRoles, String userId, String username, String firstName, String lastName, String email, String cellPhone, String personRef, net.whydah.sso.user.types.UserCredential userCredential, String userticket, HttpServletRequest request) {
		log.debug("createAndLogonUser - apptokenid: {}", getMyAppTokenID());

		WebResource createUserResource = tokenServiceClient.resource(uri_securitytoken_service).path("user/" + getMyAppTokenID() + "/" + userticket + "/create_user");
		log.debug("createUserResource:"+createUserResource.toString());

		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		formData.add("apptoken", getMyAppTokenXml());
		formData.add("usercredential", userCredential.toXML());
		String userXml = getUserXml(provider, accessToken, appRoles, userId, firstName, lastName, username, email, cellPhone, personRef);
		formData.add("userxml", userXml);
		log.trace("createAndLogonUser with User XML: " + userXml +"\nformData:\n"+formData);
		log.info("createAndLogonUser username=" + username);
		ClientResponse response = createUserResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);

		//No need to retry if we know it is forbidden.
		if (response.getStatus() == FORBIDDEN.getStatusCode()) {
			//throw new IllegalArgumentException("createAndLogonUser failed. username=" + fbUser.getUsername() + ", id=" + fbUser.getId());
			log.warn("createAndLogonUser failed. username=" + username + ", email=" + email);
			return null;
		}
		if (response.getStatus() == OK.getStatusCode()) {
			String responseXML = response.getEntity(String.class);
			log.debug("createAndLogonUser OK with response {}", responseXML);
			SessionDao.instance.updateApplinks();
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
	}

	private String getProviderAccessTokenTag(String provider, String accessToken) {
		if(provider!=null) {
			if(provider.equalsIgnoreCase("aad")) {
				return "		 <aadAccessToken>" + accessToken + "</aadAccessToken>";
			} else if(provider.equalsIgnoreCase("fb")) {
				return "		 <fbAccessToken>" + accessToken + "</fbAccessToken>";
			} else if(provider.equals("netiq")) {
				return "		 <netIQAccessToken>" + accessToken + "</netIQAccessToken>";
			} else if(provider.equals("google")) {
				return "		 <googleAccessToken>" + accessToken + "</googleAccessToken>";
			} else if(provider.equals("rebel")) {
				return "		 <rebelAccessToken>" + accessToken + "</rebelAccessToken>";
			}
		}
		return "";
	}
	
	public String getUserXml(String provider, String accessToken, String appRoles, String userId, String firstName, String lastName, String username, String email, String cellPhone, String personRef) {
		StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
		strb.append("<user>\n");
		strb.append("    <params>\n");
		strb.append(getProviderAccessTokenTag(provider, accessToken) + "\n");
		strb.append("		 <appRoles>").append(appRoles).append("</appRoles>\n");
		strb.append("        <userId>").append(userId).append( "</userId>\n");
		strb.append("        <firstName>").append(firstName).append("</firstName>\n");
		strb.append("        <lastName>").append(lastName).append("</lastName>\n");
		strb.append("        <username>").append(username).append("</username>\n");  // +UUID.randomUUID().toString()
		strb.append("        <email>").append(email).append( "</email>\n");
		strb.append("        <cellPhone>").append(cellPhone).append( "</cellPhone>\n");
		strb.append("        <personRef>").append(personRef).append( "</personRef>\n");
		strb.append("    </params> \n");
		strb.append("</user>\n");
		log.info(strb.toString());
		return strb.toString();
	}


}


