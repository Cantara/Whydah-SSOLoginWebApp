package net.whydah.sso.authentication.oidc.providers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nimbusds.oauth2.sdk.GeneralException;

import net.whydah.sso.authentication.oidc.LoginController;
import net.whydah.sso.config.AppConfig;

@Controller
public class Microsoft implements Provider {
    public static final String provider = "azuread";

    private final LoginController controller;

    @Autowired
    public Microsoft() throws IOException, GeneralException, URISyntaxException {
    	//TODO: check again
        //Properties properties = AppConfig.readProperties();
        this.controller = null;// new LoginController(provider);
    }

    @RequestMapping("/" + provider + "/login")
    public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.login(httpRequest, httpResponse, model);
    }

    @RequestMapping("/" + provider + "/auth")
    public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.authenticate(httpRequest, httpResponse, model);
    }

    @RequestMapping("/" + provider + "/basicinfo_confirm")
    public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmBasicInfo(httpRequest, httpResponse, model);
    }

    @RequestMapping("/" + provider + "/credential_confirm")
    public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmExist(httpRequest, httpResponse, model);
    }
}
