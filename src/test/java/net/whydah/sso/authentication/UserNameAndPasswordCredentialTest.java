package net.whydah.sso.authentication;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;

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