package net.whydah.sso.authentication;

import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UserNameAndPasswordCredentialTest {

    @Test
    public void testOKUserNameAndPassword() {
        UserName userName = new UserName("useradmin");
        Password password = new Password("useradmin42");

        UserNameAndPasswordCredential userNameAndPasswordCredential = new UserNameAndPasswordCredential(userName, password);
        UserNameAndPasswordCredential userNameAndPasswordCredential2 = new UserNameAndPasswordCredential("useradmin", "useradmin42");
        assertTrue(new UserNameAndPasswordCredential("useradmin", "useradmin42") != null);
    }

}