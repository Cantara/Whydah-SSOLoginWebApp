package net.whydah.sso.authentication.iamproviders.google;

import java.util.Properties;

public class GoogleConnectionProperties {

    
    private String clientId=null;
    private String clientSecret=null;
    private String redirectUriSignIn;

    public GoogleConnectionProperties(String ssoURL, Properties properties) {
    	
        redirectUriSignIn = ssoURL + "googleauth"; 
        
        if(properties!=null) {
        	clientId = properties.getProperty("google.clientid");
        	clientSecret = properties.getProperty("google.secretkey");
        }
        //assign for testing
        if(clientId == null || clientSecret == null) {
        	clientId = "87067140729-3v9p8b3l9hkk8juhm1d2gn366ehls2uv.apps.googleusercontent.com";
        	clientSecret = "P7-cNlNi_FmZQ801g1fxWB5h";
        }
        
	}

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }


    public String getSSOCallback() {
        return redirectUriSignIn;
    }

    @Override
    public String toString() {
        return "GoogleConnectionProperties{" +
                "clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", redirectUriSignIn='" + redirectUriSignIn + '\'' +
                '}';
    }
}
