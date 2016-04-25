package net.whydah.sso.tokenservice;

import net.whydah.sso.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class RedirectURITest {

    @BeforeClass
    public static void setup() {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
    }


    @Ignore
    @Test
    public void testRedirectLogic(){
        TokenServiceClient tokenServiceClient = new TokenServiceClient();
        String redirectURI = "http://demo.getwhydah.com/test/hello";
        String userTicket = UUID.randomUUID().toString();
        // ticket on redirect
        if (redirectURI.toLowerCase().contains("userticket")) {
            // Do not overwrite ticket
        } else {
            redirectURI = tokenServiceClient.appendTicketToRedirectURI(redirectURI, userTicket);

        }
        assertTrue(redirectURI.toLowerCase().contains("userticket"));
    }
}
