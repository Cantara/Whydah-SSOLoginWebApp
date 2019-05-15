package net.whydah.sso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class LoginTypes {
	private final static Logger log = LoggerFactory.getLogger(LoginTypes.class);
	
	private static final String ENABLED = "enabled";
    private static final String TRUE = "true";

    private final boolean facebookLoginEnabled;
	private final boolean openIdLoginEnabled;
	private final boolean omniLoginEnabled;
	private final boolean userpasswordLoginEnabled;
    private final boolean newIQLoginEnabled;
    private final boolean signupEnabled;

    public LoginTypes(Properties properties) {
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

}
