package net.whydah.sso.authentication;

import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.ModelHelper;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-06
 */

public class ModelHelperTest {

    @Test
    public void testLoginTypesWiringOK() {
        for (String appMode : Arrays.asList(ApplicationMode.PROD, ApplicationMode.TEST, ApplicationMode.TEST_L, ApplicationMode.DEV)) {
            System.setProperty(ApplicationMode.IAM_MODE_KEY, appMode);
            assertNotNull(ModelHelper.enabledLoginTypes);
        }
    }
}
