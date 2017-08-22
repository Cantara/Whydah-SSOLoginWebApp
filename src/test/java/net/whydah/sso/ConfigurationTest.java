package net.whydah.sso;

import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Test;

import static net.whydah.sso.ServerRunner.printConfiguration;

public class ConfigurationTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
    }

    @Test
    public void testPrintConfiguration() throws Exception{
        printConfiguration(AppConfig.readProperties());
    }
}
