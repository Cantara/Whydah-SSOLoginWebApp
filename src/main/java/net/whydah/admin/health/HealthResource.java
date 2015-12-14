package net.whydah.admin.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.core.Response;

/**
 * Endpoint for health check, copied from UIB.
 */
@Controller
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    public HealthResource() {
    }

    @RequestMapping("/health")
    public Response isHealthy() {
        boolean ok = true;
        log.trace("isHealthy={}", ok);
        if (ok) {
            return Response.ok("Status OK!").build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
