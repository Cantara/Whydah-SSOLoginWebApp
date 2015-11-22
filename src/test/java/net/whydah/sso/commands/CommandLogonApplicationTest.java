package net.whydah.sso.commands;

import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.commands.appauth.CommandLogonApplicationWithStubbedFallback;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.application.types.ApplicationCredential;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandLogonApplicationTest {

    private static Properties properties;
    private static URI tokenServiceUri;
    private static ApplicationCredential applicationCredential;
    private static String applicationId;


    @BeforeClass
    public static void setup() throws Exception {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.TEST);
        properties = AppConfig.readProperties();
        tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
        applicationId=properties.getProperty("applicationid");

    }

    @Test
    public void testApplicationLoginCommandFallback() throws Exception {

        applicationCredential= new ApplicationCredential(applicationId,"","false secret");

        String myApplicationTokenXml = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).execute();
        // System.out.println("ApplicationTokenID=" + myApplicationTokenID);
        assertEquals("47289347982137421", ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myApplicationTokenXml));

        Future<String> fAppTokenID = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).queue();
        assertEquals("47289347982137421", ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(fAppTokenID.get()));


        Observable<String> oAppTokenID = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).observe();
        // blocking
        assertEquals("47289347982137421", ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(oAppTokenID.toBlocking().single()));
    }

    @Test
    public void testApplicationLoginCommand() throws Exception {

        applicationCredential= new ApplicationCredential("",applicationId,properties.getProperty("applicationsecret"));

        String myApplicationTokenID = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).execute();
        // System.out.println("ApplicationTokenID=" + myApplicationTokenID);
        assertTrue(myApplicationTokenID.length() > 6);

        Future<String> fAppTokenID = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).queue();
        assertTrue(fAppTokenID.get().length() > 6);

        Observable<String> oAppTokenID = new CommandLogonApplicationWithStubbedFallback(tokenServiceUri, applicationCredential).observe();
        // blocking
        assertTrue(oAppTokenID.toBlocking().single().length() > 6);
    }

}
