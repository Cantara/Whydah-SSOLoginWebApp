package net.whydah.sso;

import net.whydah.sso.authentication.CookieManager;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class ConfigHelperTest {

    @Test
    public void testCookieEncryptinFlagLogic() {
        String myuri = "http://localhost:1997/sso";
        String myurisecure = "https://localhost/sso";
        assertTrue(CookieManager.secureCookie(myurisecure));
        assertFalse(CookieManager.secureCookie(myuri));

    }


}
