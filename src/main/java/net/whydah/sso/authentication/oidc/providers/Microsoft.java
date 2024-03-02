//package net.whydah.sso.authentication.oidc.providers;
//
//import com.nimbusds.oauth2.sdk.GeneralException;
//import net.whydah.sso.authentication.oidc.LoginController;
//import net.whydah.sso.config.AppConfig;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.Properties;
//
//@RequestMapping("/"+ Microsoft.provider)
//@Controller
//public class Microsoft {
//    public static final String provider = "microsoft";
//
//    private final LoginController controller;
//
//    public Microsoft() throws IOException, GeneralException, URISyntaxException {
//        Properties properties = AppConfig.readProperties();
//        this.controller = new LoginController(provider, properties.getProperty(provider+".logoUrl"),
//                "https://login.microsoftonline.com/{tenantid}/v2.0",
//                properties.getProperty(provider+".appId"), properties.getProperty(provider+".appSecret"),
//                properties.getProperty("myuri"));
//    }
//
//    @RequestMapping("/login")
//    public String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
//        return controller.login(httpRequest, httpResponse, model);
//    }
//
//    @RequestMapping("/auth")
//    public String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
//        return controller.authenticate(httpRequest, httpResponse, model);
//    }
//
//    @RequestMapping("/basicinfo_confirm")
//    public String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
//        return controller.confirmBasicInfo(httpRequest, httpResponse, model);
//    }
//
//    @RequestMapping("/credential_confirm")
//    public String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
//        return controller.confirmExist(httpRequest, httpResponse, model);
//    }
//}
