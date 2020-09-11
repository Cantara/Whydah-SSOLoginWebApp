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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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


    public LoginTypes(Properties properties) {
    	
		enablePersonasShortcut = TRUE.equalsIgnoreCase(properties.getProperty("logintype.personasshortcut"));
		facebookLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.facebook"));
		openIdLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.openid"));
		omniLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.omni"));
        newIQLoginEnabled =  ENABLED.equalsIgnoreCase(properties.getProperty("logintype.netiq"));
		userpasswordLoginEnabled = ENABLED.equalsIgnoreCase(properties.getProperty("logintype.userpassword"));
        signupEnabled = TRUE.equalsIgnoreCase(properties.getProperty("signupEnabled"));


        log.debug(String.format("Signup is %6s, Facebook Sign on is %1s, OpenId Sign on is %2s, Omni Sign on is %3s, netIQ Sign on is %4s, User/Password Sign on is %5s."
                , properties.getProperty("signupEnabled")
                , properties.getProperty("logintype.facebook")
									, properties.getProperty("logintype.openid")
									, properties.getProperty("logintype.omni")
                                    , properties.getProperty("logintype.netiq")
                , properties.getProperty("logintype.userpassword")
                , properties.getProperty("signupEnabled")));
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
	
	
}
