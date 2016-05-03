package net.whydah.sso.authentication.facebook;

import net.whydah.sso.config.ApplicationMode;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.User;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/15/12
 */
public class FacebookHelperTest {
    private static final Logger log = LoggerFactory.getLogger(FacebookHelperTest.class);

    @Before
    public void initialize() {
      System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);

    }

    /**
     * Manual test.
     * Update access clients and run.
     * https://graph.facebook.com/me?access_token=
     */
    @Ignore
    @Test
    public void testCreateUserFromFacebookAttributes() {
        String faceBookAccessToken = "fbAccessTokenHere";
        FacebookClient facebookClient = new DefaultFacebookClient(faceBookAccessToken);
        User fbUser = facebookClient.fetchObject("me", User.class);

        assertEquals("745925301", fbUser.getId());
        assertEquals("Erik", fbUser.getFirstName());
        assertEquals("Drolshammer", fbUser.getLastName());
        assertEquals("male", fbUser.getGender());
        assertEquals("Moss, Norway", fbUser.getHometown().getName());
        assertEquals("Oslo, Norway", fbUser.getLocation().getName());

        //Requires more permissions
        assertEquals("08/05/1982", fbUser.getBirthday());
        assertEquals("erik.drolshammer@gmail.com", fbUser.getEmail());
    }


    /**
     * Manual test.
     */
    @Ignore
    @Test
    public void testGetFacebookFriends() {
        String faceBookAccessToken = "fbAccessTokenHere";
        FacebookClient facebookClient = new DefaultFacebookClient(faceBookAccessToken);

        Connection<User> friendsConnection = facebookClient.fetchConnection("me/friends", User.class, Parameter.with("fields", "id, name, birthday"));
        @SuppressWarnings("unused")
		List<User> users = friendsConnection.getData();
    }

    @Test
    public void testGetFacebookUserAsXml() {

        String fbAssessToken = "accessMe1234567";
        User fbUser = mock(User.class);
        String fbUserXml = FacebookHelper.getFacebookUserAsXml(fbUser,fbAssessToken);
        log.debug("fbUserXml: {}" , fbUserXml);
        assertNotNull(fbUserXml);

    }

}
