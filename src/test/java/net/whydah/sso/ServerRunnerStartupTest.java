package net.whydah.sso;

import net.whydah.sso.config.ApplicationMode;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple test to reproduce the production startup failure:
 * "NoClassDefFoundError: jakarta/servlet/Servlet"
 */
public class ServerRunnerStartupTest {

    @Test
    public void testServerRunnerCanStart() throws Exception {
        // Set up the environment like production
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);

        // Try to create and start ServerRunner - this should work or fail clearly
        ServerRunner serverRunner = null;
        try {
            serverRunner = new ServerRunner(0); // Use port 0 for auto-allocation
            serverRunner.start();

            // If we get here, it worked
            assertTrue("ServerRunner started successfully", true);

        } finally {
            // Clean up
            if (serverRunner != null) {
                try {
                    serverRunner.stop();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }
}