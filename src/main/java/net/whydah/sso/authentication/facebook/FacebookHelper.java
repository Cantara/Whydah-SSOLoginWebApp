package net.whydah.sso.authentication.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;
import net.whydah.sso.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;

/**
 * http://developers.facebook.com/docs/authentication/server-side/
 *
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/15/12
 */
public class FacebookHelper {
    // get these from your FB Dev App - Erik's WhydahTest app
    static  String FACEBOOK_APP_ID = "YOUHAVEFORGOTTENTOCONFIGURE_FACEBOOK_APP_ID";
    static  String FACEBOOK_APP_SECRET = "YOUHAVEFORGOTTENTOCONFIGURE_FACEBOOK_APP_SECRET";
    static  String FACEBOOK_FALLBACKEMAIL = "dummy@email.com";
    //TODO Remember to remove permissions we don't use.
    //http://developers.facebook.com/docs/authentication/permissions/#user_friends_perms
    static final String FACEBOOK_PERMISSIONS_USER = "user_about_me,user_birthday,user_hometown,user_location,email";
    static final String FACEBOOK_PERMISSIONS_FRIENDS = "friends_about_me,friends_birthday,friends_hometown,friends_location";

    static final String FACEBOOK_PERMISSIONS = FACEBOOK_PERMISSIONS_USER + "," + FACEBOOK_PERMISSIONS_FRIENDS;

    private static final Logger log = LoggerFactory.getLogger(FacebookHelper.class);
    static {
            try {
                Properties properties = AppConfig.readProperties();
                FACEBOOK_APP_ID = properties.getProperty("FACEBOOK_APP_ID");
                FACEBOOK_APP_SECRET = properties.getProperty("FACEBOOK_APP_SECRET");
                FACEBOOK_FALLBACKEMAIL= properties.getProperty("FACEBOOK_FALLBACKEMAIL");

                log.info("read new Facebook appdata form properties ");
            } catch (IOException notFound) {
                log.error("Error - not able to load facebook appdata from configuration.  Using embedded as fallback");
            }
        }




    /**
     * Access the following required information:
     Your email address (erik.drolshammer@gmail.com)
     Your birthday
     Friends' descriptions
     Friends' birthdays
     Access your custom friend lists
     * @param clientRedirectURI
     * @param fbauthURI
     * @return
     */
    public static String getFacebookLoginUrl(String clientRedirectURI, String fbauthURI) {
        String facebookLoginUrl = "https://graph.facebook.com/oauth/authorize?client_id=" + FACEBOOK_APP_ID +
                "&display=page&redirect_uri=" + fbauthURI + "&state=" + clientRedirectURI + "&scope=" + FACEBOOK_PERMISSIONS;
        log.debug("facebookLoginUrl: {}", facebookLoginUrl);
        return facebookLoginUrl;
    }

    public static Map.Entry<String, User> loginAndCreateFacebookUser(String code, String fbauthURI) {
        String fbAccessToken = getAccessToken(code, fbauthURI);
        //FacebookUser fbUser = createUserFromFacebookAttributes(faceBookAccessToken);
        FacebookClient facebookClient = new DefaultFacebookClient(Version.LATEST);
        facebookClient.createClientWithAccessToken(fbAccessToken);
        User fbUser = facebookClient.fetchObject("me", User.class);
        log.trace("fbUser {}",fbUser);
        Map.Entry<String, User> pair = new AbstractMap.SimpleImmutableEntry<>(fbAccessToken, fbUser);
        log.debug("Logged in Facebook user: code=" + code + ", fbAccessToken=" + fbAccessToken + "\n fbUser: " + fbUser.toString());
        return pair;
    }

    /**
     * Will produce a xml looking like this
     * @param fbUser - the facebook user object
     * @param fbAccessToken - the facebook access token
     * @return - our representation of the facebook user data
     */
    public static String getFacebookUserAsXml(User fbUser, String fbAccessToken) {
        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>\n");
        strb.append("    <params>\n");

        strb.append("        <fbAccessToken>").append(fbAccessToken).append("</fbAccessToken>\n");

        strb.append("        <userId>").append(fbUser.getId()).append("</userId>\n");
        String firstName = fbUser.getFirstName();
        if (fbUser.getMiddleName() != null) {
            firstName += " ";
            firstName += fbUser.getMiddleName();
        }
        strb.append("        <firstName>").append(firstName).append("</firstName>\n");
        strb.append("        <lastName>").append(fbUser.getLastName()).append("</lastName>\n");
        if (fbUser.getId() != null && fbUser.getId().length() > 2) {
            strb.append("        <username>").append(fbUser.getId()).append("</username>\n");
        } else if (fbUser.getEmail() != null && fbUser.getEmail().length() > 2) {
            log.warn("Facebook returned username = null, using email as username ");
            strb.append("        <username>").append(fbUser.getEmail()).append("</username>\n");
        } else if (fbUser.getId() != null && fbUser.getId().length() > 2) {
            log.warn("Facebook returned username and email = null, using id as username ");
            strb.append("        <username>").append(fbUser.getId()).append("</username>\n");
        }
        strb.append("        <gender>").append(fbUser.getGender()).append("</gender>\n");
        if (fbUser.getEmail() != null && fbUser.getEmail().length() > 2) {
            strb.append("        <email>").append(fbUser.getEmail()).append("</email>\n");
        } else {
            log.warn("Facebook returned email = null, using dummy@email.com as email ");
            strb.append("        <email>").append(FACEBOOK_FALLBACKEMAIL).append( "</email>\n");
        }
        strb.append("        <birthday>").append(fbUser.getBirthday()).append( "</birthday>\n");
        strb.append("        <hometown>").append(fbUser.getHometownName()).append( "</hometown>\n");

        if (fbUser.getLocation() != null) {
            strb.append("        <location>").append(fbUser.getLocation().getName()).append( "</location>\n");
        }

        strb.append("    </params> \n");
        strb.append("</user>\n");
        return strb.toString();
    }


    private static String getAccessToken(String code, String fbauthURI) {
        log.info("Fetching access token from facebook with code " + code);
        if (code == null || code.equals("")) {
            log.debug("No facebook code, returning to login.");
            return null;
        }

        String result;
        try {
            String authURL = getAuthURL(code, fbauthURI);
            URL url = new URL(authURL);
            result = readURL(url);
        } catch (Exception e) {
            log.error("Error autenticating with facebook.", e);
            return null;
        }

        String faceBookAccessToken = null;
        Integer expires = null;
        String[] pairs = result.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length != 2) {
                throw new RuntimeException("Unexpected auth response");
            } else {
                if (kv[0].equals("access_token")) {
                    faceBookAccessToken = kv[1];
                }
                if (kv[0].equals("expires")) {
                    expires = Integer.valueOf(kv[1]);
                }
            }
        }

        log.debug("faceBookAccessToken=" + faceBookAccessToken + ", expires=" + expires);
        return faceBookAccessToken;
    }

    private static String getAuthURL(String authCode, String fbauthURI) {
        return "https://graph.facebook.com/oauth/access_token?client_id=" + FACEBOOK_APP_ID +
                "&redirect_uri=" + fbauthURI + "&client_secret=" + FACEBOOK_APP_SECRET + "&code="+authCode;
    }

    private static String readURL(URL url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = url.openStream();
        int r;
        while ((r = is.read()) != -1) {
            baos.write(r);
        }
        return new String(baos.toByteArray());
    }

}
