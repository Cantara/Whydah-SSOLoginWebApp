package net.whydah.sso.authentication.oidc;

import com.nimbusds.oauth2.sdk.GeneralException;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.net.URISyntaxException;

public class Twitch {
    SessionManagementHelper sessionManagementHelper;
    AuthHelper authHelper;
    @Test
    public void testCreateAuthHelper() throws Throwable {
        String provider = "twitch";
        String appUri = "http://localhost:3030";
        String issuerUrl = "https://id.twitch.tv/oauth2";
        String appId = "c96cnydm3b38ig3k51z2fohuf4vbbg";
        String appSecret = "tcj5p60xyyq9kf1cx1z6z4yp7h57gp";
        this.sessionManagementHelper = new SessionManagementHelper(provider);
        this.authHelper = new AuthHelper(appUri, provider, issuerUrl, appId, appSecret, new String[]{"openid"}, this.sessionManagementHelper);
    //}

    //@Test
    //public void testCreateRedirectURL() {
        System.out.println(authHelper.getAuthorizationCodeUrl("UEoq7ycL4AGxnuVh3oJ8q6ldJzSRUTc00qMNctJWymw", "UEoq7ycL4AGxnuVh3oJ8q6ldJzSRUTc00qMNctJWymw"));//AuthHelper.newState(), AuthHelper.newNonce()));

        System.out.println(authHelper.processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl("http://localhost:3030/twitch/auth?code=a8tt81um80074yzsumriuno9o4byjn&scope=openid&state=UEoq7ycL4AGxnuVh3oJ8q6ldJzSRUTc00qMNctJWymw"));
    }
}
