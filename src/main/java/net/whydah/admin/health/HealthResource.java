package net.whydah.admin.health;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.DEFCON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

@Controller
public class HealthResource {
    private static WhydahServiceClient serviceClient = SessionDao.instance.getServiceClient();

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    protected static Properties properties;

    private static String applicationInstanceName;
    public static boolean ok = false;



    public HealthResource()  {
        try {
            properties = AppConfig.readProperties();
            applicationInstanceName = properties.getProperty("applicationname");

        } catch (Exception e){
            log.warn("Unable to create WhydahServiceClient in constructor",e);
        }

    }

    @RequestMapping(value = "/health", produces = MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity isHealthy(HttpServletRequest request, HttpServletResponse response, Model model) {
        if (serviceClient==null){
            try {
                properties = AppConfig.readProperties();
                serviceClient = new WhydahServiceClient();

            } catch (Exception f){
                log.warn("Unable to create WhydahServiceClient",f);
            }

        }
        try {
            if (serviceClient.getWAS()==null){
                return ResponseEntity.ok("Initializing");
            }
            ok = serviceClient.getWAS().getDefcon().equals(DEFCON.DEFCON5);

            if (ok && serviceClient.getWAS().checkActiveSession()) {
                log.trace("isHealthy={}, status: {}", ok, WhydahUtil.getPrintableStatus(serviceClient.getWAS()));
                model.addAttribute(ConstantValue.HEALTH, getHealthTextJson());
                return ResponseEntity.ok(getHealthTextJson());
            } else {
                model.addAttribute(ConstantValue.HEALTH, "{\n  \"isHealthy\": \"false\"\n}");
                return ResponseEntity.ok("{\"isHealthy\": \"false\"}");
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e){
            log.warn("Initializing WhydahServiceClient",e);
            model.addAttribute(ConstantValue.HEALTH, "Initializing WhydahServiceClient");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        }
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"" + ok + "\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + serviceClient.getWAS().getDefcon() + "\",\n" +
                "  \"hasApplicationToken\": \"" + Boolean.toString(serviceClient.getWAS().getActiveApplicationTokenId() != null) + "\",\n" +
               // "  \"hasValidApplicationToken\": \"" + Boolean.toString(serviceClient.getWAS().checkActiveSession()) + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + Boolean.toString(serviceClient.getWAS().hasApplicationMetaData()) + "\",\n" +
                "  \"now\": \"" + Instant.now() + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +
                "  \"" + ConstantValue.SECURITYTOKENSERVICEHEALTH + "\": \"" + properties.getProperty("securitytokenservice") + "health" + "\" ,\n" +
                "  \"" + ConstantValue.USERADMINSERVICEHEALTH + "\": \"" + properties.getProperty("useradminservice") + "health" + "\" ,\n" +
                "  \"" + ConstantValue.STATISTICSSERVICEHEALTH + "\": \"" + properties.getProperty("reportservice") + "health" + "\" ,\n" +
                "  \"" + ConstantValue.CRMSERVICEHEALTH + "\": \"" + properties.getProperty("crmservice") + "health" + "\" \n" +
                "}\n";
    }


    private String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.sso/SSOLoginWebApp/pom.properties";
        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }
}

