package net.whydah.sso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import edu.emory.mathcs.backport.java.util.Arrays;
import net.whydah.sso.model.PersonaCredential;
import net.whydah.sso.user.types.UserCredential;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoginTypes {
	private final static Logger log = LoggerFactory.getLogger(LoginTypes.class);
	
	private static final String ENABLED = "enabled";
    private static final String TRUE = "true";

    private boolean facebookLoginEnabled;
	private boolean openIdLoginEnabled;
	private boolean omniLoginEnabled;
	private boolean userpasswordLoginEnabled;
    private boolean newIQLoginEnabled;
    private boolean signupEnabled;
    private boolean enablePersonasShortcut=false;
    private boolean googleLoginEnabled;
    private boolean rebelLoginEnabled;
    private boolean microsoftLoginEnabled;
    //sign up
    private boolean signuppageGoogleOn = false;
    private boolean signuppageFacebookOn = false;
    private boolean signuppageWhydahIntegrationproviderOn = false;
    private boolean signuppageNetIQOn = false;
    private boolean signuppageMicrosoftOn = false;
	private final Set<String> oidcProvidersEnabled = new HashSet<>();

    public LoginTypes(Properties properties) {
		enablePersonasShortcut = TRUE.equalsIgnoreCase(properties.getProperty("logintype.personasshortcut"));
		facebookLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.facebook"));
		openIdLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.openid"));
		omniLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.omni"));
        newIQLoginEnabled =  ENABLED.equalsIgnoreCase(properties.getProperty("logintype.netiq"));
		userpasswordLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.userpassword"));
        signupEnabled = TRUE.equalsIgnoreCase(properties.getProperty("signupEnabled"));
        googleLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.google"));
        rebelLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.rebel"));
        microsoftLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.microsoft"));
        
        //sign up
        signuppageGoogleOn = "ON".equalsIgnoreCase(properties.getProperty("signuppage.google"));
        signuppageWhydahIntegrationproviderOn = "ON".equalsIgnoreCase(properties.getProperty("signuppage.whydah.integration.providers"));
        signuppageFacebookOn = "ON".equalsIgnoreCase(properties.getProperty("signuppage.facebook"));
        signuppageNetIQOn = "ON".equalsIgnoreCase(properties.getProperty("signuppage.netiq"));
        signuppageMicrosoftOn = "ON".equalsIgnoreCase(properties.getProperty("signuppage.microsoft"));
        
        log.debug("Signup is {} "
        		+ "- Google Sign on is {}"
        		+ "- Microsoft Sign on is {}"
        		+ "- Facebook Sign on is {}"
        		+ "- OpenId Sign on is {}"
        		+ "- Omni Sign on is {}"
        		+ "- NetIQ Sign on is {}"
        		+ "- User/Password Sign on is {}."
                , properties.getProperty("signupEnabled")
                , properties.getProperty("logintype.google")
                , properties.getProperty("logintype.microsoft")
                , properties.getProperty("logintype.facebook")
				, properties.getProperty("logintype.openid")
				, properties.getProperty("logintype.omni")
                , properties.getProperty("logintype.netiq")
                , properties.getProperty("logintype.userpassword")
        );
    }

    public boolean isSignupEnabled() {
        return signupEnabled;
    }

    public boolean isFacebookLoginEnabled() {
		return facebookLoginEnabled;
	}

	public boolean isOpenIdLoginEnabled() {
		return openIdLoginEnabled;
	}

    public boolean isNetIQLoginEnabled() {
        return newIQLoginEnabled;
    }

	public boolean isOmniLoginEnabled() {
		return omniLoginEnabled;
	}

	public boolean isUserpasswordLoginEnabled() {
		return userpasswordLoginEnabled;
	}
	
	public boolean isPersonasShortcutEnabled() {
		return enablePersonasShortcut;
	}
	
	public boolean isGoogleLoginEnabled() {
		return googleLoginEnabled;
	}
	
	public boolean isMicrosoftLoginEnabled() {
		return microsoftLoginEnabled;
	}
	
	public boolean isRebelLoginEnabled() {
		return rebelLoginEnabled;
	}

	public boolean isSignuppageGoogleOn() {
		return signuppageGoogleOn;
	}

	public boolean isSignuppageFacebookOn() {
		return signuppageFacebookOn;
	}

	public boolean isSignuppageWhydahIntegrationproviderOn() {
		return signuppageWhydahIntegrationproviderOn;
	}

	public boolean isSignuppageNetIQOn() {
		return signuppageNetIQOn;
	}
	
	public boolean isSignuppageMicrosoftOn() {
		return signuppageMicrosoftOn;
	}

	public void addOIDCProvider(String provider) {
		oidcProvidersEnabled.add(provider);
	}

	public boolean isOIDCProviderEnabled(String provider) {
		return oidcProvidersEnabled.contains(provider);
	}

	public Set<String> getOIDCProvidersEnabled() {
		return oidcProvidersEnabled;//not sure i like this, would like a copy.
	}
}
