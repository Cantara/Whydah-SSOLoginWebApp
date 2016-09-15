package net.whydah.admin.health;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.WhydahUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

@Controller
public class HealthResource {
    private final WhydahServiceClient serviceClient;

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    protected Properties properties;


    public HealthResource() throws Exception {
        this.serviceClient = new WhydahServiceClient();
        properties = AppConfig.readProperties();

    }

    @RequestMapping("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy(HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean ok = serviceClient.getWAS().getDefcon().equals(UserToken.DEFCON.DEFCON5);

        log.trace("isHealthy={}, status: {}", ok, WhydahUtil.getPrintableStatus(serviceClient.getWAS()));
        if (ok) {
            model.addAttribute(ConstantValue.HEALTH, getHealthTextJson());
            return Response.ok(getHealthTextJson()).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + serviceClient.getWAS().getDefcon() + "\",\n" +
                "  \"hasApplicationToken\": \"" + Boolean.toString(serviceClient.getWAS().getActiveApplicationTokenId() != null) + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + Boolean.toString(serviceClient.getWAS().checkActiveSession()) + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + Boolean.toString(serviceClient.getWAS().getApplicationList().size() > 2) + "\",\n" +
                "  \"" + ConstantValue.SECURITYTOKENSERVICEHEALTH + "\": " + properties.getProperty("securitytokenservice") + "health" + ",\n" +
                "  \"" + ConstantValue.USERADMINSERVICEHEALTH + "\": " + properties.getProperty("useradminservice") + "health/" + ",\n" +
                "  \"" + ConstantValue.CRMSERVICEHEALTH + "\": " + properties.getProperty("crmservice") + "health" + "\n" +
                "}\n";
    }


    private String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.sso/SSOLoginWebApp/pom.properties";
        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)";
    }
}

