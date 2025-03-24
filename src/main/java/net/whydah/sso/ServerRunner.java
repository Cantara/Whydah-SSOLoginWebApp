package net.whydah.sso;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.Servlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.util.SSLTool;

public class ServerRunner {
	public static int PORT_NO = 9997;
	public static int SSL_PORT_NO = 8443;
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

//TODO: too difficult to integrate now 
//		if(ApplicationMode.getApplicationMode().equalsIgnoreCase("prod")) {
//			ApplicationProperties.builder()
//			.classpathPropertiesFile("ssologinservice.PROD.properties")
//			.filesystemPropertiesFile("ssologinservice.PROD.properties")	
//			.buildAndSetStaticSingleton();
//		}
//		else if(ApplicationMode.getApplicationMode().equalsIgnoreCase("test")) {
//			ApplicationProperties.builder()
//			.classpathPropertiesFile("ssologinservice.TEST.properties")
//			.filesystemPropertiesFile("ssologinservice.TEST.properties")	
//			.buildAndSetStaticSingleton();
//		}
//		else if(ApplicationMode.getApplicationMode().equalsIgnoreCase("test_localhost")) {
//			ApplicationProperties.builder()
//			.classpathPropertiesFile("ssologinservice.TEST_LOCALHOST.properties")
//			.filesystemPropertiesFile("ssologinservice.TEST_LOCALHOST.properties")
//			.expectedProperties(SecurityProperties.class)
//			.buildAndSetStaticSingleton();
//		}
//		else if(ApplicationMode.getApplicationMode().equalsIgnoreCase("dev")) {
//			ApplicationProperties.builder()
//			.classpathPropertiesFile("ssologinservice.DEV.properties")
//			.filesystemPropertiesFile("ssologinservice.DEV.properties")	
//			.buildAndSetStaticSingleton();
//		}

		ServerRunner serverRunner = new ServerRunner();
		serverRunner.start();
		printConfiguration(AppConfig.readProperties());
		serverRunner.join();
		SSLTool.disableCertificateValidation();
//		WhydahServiceClient tc = new WhydahServiceClient();
		log.info("SSOLoginWebApp started OK. Version = {},IAM_MODE = {}, url: http://localhost:{}{}/login",
				version, ApplicationMode.getApplicationMode(), String.valueOf(PORT_NO), CONTEXT);

	}

	private Connector getSSLConnector() {
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());
		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(ServerRunner.class.getResource("/keystore.jks").toExternalForm());
		sslContextFactory.setKeyStorePassword("123456");
		sslContextFactory.setKeyManagerPassword("123456");

		ServerConnector sslConnector = new ServerConnector(server, 
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(https)
				);
		sslConnector.setPort(SSL_PORT_NO);
		return sslConnector;
	}

	public ServerRunner() throws IOException {
		this(PORT_NO);
	}

	public ServerRunner(int port) throws IOException {
		PORT_NO = port;
		server=new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		if(ApplicationMode.getApplicationMode().equals(ApplicationMode.TEST_L)) {
			server.setConnectors(new Connector[] {connector, getSSLConnector()});
		} else {
			server.setConnectors(new Connector[] {connector});
		}

		ROOT_URL = "http://localhost:" + PORT_NO + CONTEXT;
		TESTURL = ROOT_URL + "/action";

		context = new ServletContextHandler(server, CONTEXT, ServletContextHandler.SESSIONS);
		context.setSessionHandler(new SessionHandler());
		version = this.getClass().getPackage().getImplementationVersion();

		//MetricRegistry metrics = (MetricRegistry) context.getAttribute("com.codahale.metrics.servlets.MetricsServlet.registry");

		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		dispatcherServlet.setContextConfigLocation("classpath:webapp/sso/mvc-config.xml");
		ServletHolder servletHolder = new ServletHolder((Servlet) dispatcherServlet);
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
