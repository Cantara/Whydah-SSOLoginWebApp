package net.whydah.admin.health;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.ConstantValue;
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
import java.util.Properties;

@Controller
public class HealthResource {
    private final WhydahServiceClient tokenServiceClient;

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    protected Properties properties;


    public HealthResource() throws Exception {
        this.tokenServiceClient = new WhydahServiceClient();
        properties = AppConfig.readProperties();

    }

    @RequestMapping("/health")
    @Produces(MediaType.TEXT_HTML)
    public Response isHealthy(HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean ok = true;
        String statusText = WhydahUtil.getPrintableStatus(tokenServiceClient.getWAS());

        log.trace("isHealthy={}, status: {}", ok, statusText);
        if (ok) {
            model.addAttribute("health", "Status OK!");
            try {
                model.addAttribute(ConstantValue.SECURITYTOKENSERVICEHEALTH, properties.getProperty("securitytokenservice") + "health");
                model.addAttribute(ConstantValue.USERADMINSERVICEHEALTH, properties.getProperty("useradminservice") + "health/");
                model.addAttribute(ConstantValue.CRMSERVICEHEALTH, properties.getProperty("crmservice") + "health");
            } catch (Exception e) {
                log.warn("Unable to read properties");
            }

            return Response.ok("Status OK!\n" + statusText).build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            model.addAttribute("health", "Status Error");
            try {
                model.addAttribute(ConstantValue.SECURITYTOKENSERVICEHEALTH, properties.getProperty("securitytokenservice") + "health");
                model.addAttribute(ConstantValue.USERADMINSERVICEHEALTH, properties.getProperty("useradminservice") + "health/");
                model.addAttribute(ConstantValue.CRMSERVICEHEALTH, properties.getProperty("crmservice") + "health");
            } catch (Exception e) {
                log.warn("Unable to read properties");
            }

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

