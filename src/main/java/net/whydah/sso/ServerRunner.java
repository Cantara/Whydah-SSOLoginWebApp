package net.whydah.sso;

import com.codahale.metrics.servlets.AdminServlet;
import net.whydah.sso.config.ApplicationMode;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;

public class ServerRunner {
    public static final int PORT_NO = 9997;
    public static final String CONTEXT = "/sso";
    public static final String ROOT_URL = "http://localhost:" + PORT_NO + CONTEXT;
    public static final String TESTURL = ROOT_URL + "/action";

    private static final Logger log = LoggerFactory.getLogger(ServerRunner.class);

    private Server server;
    private ServletContextHandler context;
    public static String version;
    //private static Properties appConfig;

    public static void main(String[] arguments) throws Exception {
        ServerRunner serverRunner = new ServerRunner();
        serverRunner.start();


        int port = serverRunner.server.getConnectors()[0].getLocalPort();
        log.info("SSOLoginWebApp started OK. Version = {},IAM_MODE = {}, url: http://localhost:{}{}/login",
                version, ApplicationMode.getApplicationMode(), String.valueOf(port), CONTEXT);
        serverRunner.join();
    }

    public ServerRunner() throws IOException {
        server = new Server(PORT_NO);
        context = new ServletContextHandler(server, CONTEXT);
        version = this.getClass().getPackage().getImplementationVersion();

        context.addServlet(new ServletHolder(new AdminServlet()), "/metrics/*");

        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setContextConfigLocation("classpath:webapp/sso/mvc-config.xml");
        ServletHolder servletHolder = new ServletHolder(dispatcherServlet);
        context.addServlet(servletHolder, "/*");
    }

    public void start() throws Exception {
        server.start();
    }
    public void stop() throws Exception {
        server.stop();
    }
    public void join() throws InterruptedException {
        server.join();
    }
}
