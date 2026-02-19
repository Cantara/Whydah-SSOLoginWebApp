package net.whydah.sso.authentication.oidc.providers;

import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nimbusds.oauth2.sdk.GeneralException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.whydah.sso.authentication.oidc.LoginController;

//@RequestMapping("/"+ Vipps.provider)
//@Component
@Controller
public class Vipps implements Provider {
    public static final String provider = "vipps";

    private final LoginController controller;

    @Autowired
    public Vipps() throws IOException, GeneralException, URISyntaxException {
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

    @RequestMapping("/" + provider + "/basicinfo_confirm")
    public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmBasicInfo(httpRequest, httpResponse, model);
    }

    @RequestMapping("/" + provider + "/credential_confirm")
    public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        return controller.confirmExist(httpRequest, httpResponse, model);
    }
}
