package net.whydah.sso;

import com.codahale.metrics.MetricRegistry;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.util.SSLTool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.util.Properties;

public class ServerRunner {
    public static int PORT_NO = 9997;
    public static final String CONTEXT = "/sso";
    public static String ROOT_URL = "http://localhost:" + PORT_NO + CONTEXT;
    public static String TESTURL = ROOT_URL + "/action";

    public static String getHEALTHURL() {
        return "http://localhost:" + PORT_NO + CONTEXT + "/health";
    }

    private static final Logger log = LoggerFactory.getLogger(ServerRunner.class);

    private Server server;
    private ServletContextHandler context;
    public static String version;
    //private static Properties appConfig;

    public static void main(String[] arguments) throws Exception {
        ServerRunner serverRunner = new ServerRunner();
        serverRunner.start();


        printConfiguration(AppConfig.readProperties());

        int port = serverRunner.server.getConnectors()[0].getLocalPort();
        serverRunner.join();
        SSLTool.disableCertificateValidation();
        WhydahServiceClient tc = new WhydahServiceClient();
        log.info("SSOLoginWebApp started OK. Version = {},IAM_MODE = {}, url: http://localhost:{}{}/login",
                version, ApplicationMode.getApplicationMode(), String.valueOf(port), CONTEXT);

    }

    public ServerRunner() throws IOException {
        this(PORT_NO);
    }
    public ServerRunner(int portNo) throws IOException {
        this.PORT_NO=portNo;
        server=new Server(PORT_NO);
        context = new ServletContextHandler(server, CONTEXT);
        version = this.getClass().getPackage().getImplementationVersion();

        MetricRegistry metrics = (MetricRegistry) context.getAttribute("com.codahale.metrics.servlets.MetricsServlet.registry");

//        context.addServlet(new ServletHolder(new AdminServlet()), "/metrics/*");

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

    public static void printConfiguration(Properties properties) {
        for (Object key : properties.keySet()) {
            log.info("Using Property: {}, value: {}", key, properties.get(key));
        }
    }
}
