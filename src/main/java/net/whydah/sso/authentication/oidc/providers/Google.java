package net.whydah.sso.authentication.oidc.providers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nimbusds.oauth2.sdk.GeneralException;

import net.whydah.sso.authentication.oidc.LoginController;
import net.whydah.sso.config.AppConfig;

@Controller
public class Google implements Provider {
    public static final String provider = "google";

    private final LoginController controller;

    @Autowired
    public Google() throws IOException, GeneralException, URISyntaxException {
        Properties properties = AppConfig.readProperties();
        this.controller = new LoginController(provider);
    }

    @RequestMapping("/" + provider + "/login")
    public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.login(httpRequest, httpResponse, model);
    }

    @RequestMapping("/" + provider + "/auth")
    public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.authenticate(httpRequest, httpResponse, model);
    }

    @PostMapping("/" + provider + "/basicinfo_confirm")
    public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmBasicInfo(httpRequest, httpResponse, model);
    }

    @PostMapping("/" + provider + "/credential_confirm")
    public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmExist(httpRequest, httpResponse, model);
    }
}
