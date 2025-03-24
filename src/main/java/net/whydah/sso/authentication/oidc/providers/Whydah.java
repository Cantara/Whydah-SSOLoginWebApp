package net.whydah.sso.authentication.oidc.providers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nimbusds.oauth2.sdk.GeneralException;

import net.whydah.sso.authentication.iamproviders.WhydahOauthIntegrationConfig;
import net.whydah.sso.authentication.iamproviders.WhydahProvider;
import net.whydah.sso.authentication.oidc.LoginController;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.SessionDao;


@Controller
public class Whydah {
    
    private Map<String, LoginController> controller_map = new HashMap<>();
    
    Map<String, WhydahProvider> PROVIDERS;
    
    @Autowired
    public Whydah() throws IOException, GeneralException, URISyntaxException {
        Properties properties = AppConfig.readProperties();
        PROVIDERS = new WhydahOauthIntegrationConfig(properties).getProviderMap();
        for(WhydahProvider p : PROVIDERS.values()) {
        	controller_map.put(p.getProvider(), new LoginController(p));
        }
    }
    
    private String toSSOLWALogin(HttpServletRequest httpRequest, Model model) {
    	String redirectURI = httpRequest.getParameter("redirectURI");
        
        String params ="";
     
        if(redirectURI!=null) {
        	params += (params.equals("")?"":"&") + "redirectURI=" + redirectURI;
        }
        
        String ssolwaReDirectURI = SessionDao.instance.LOGIN_URI + (params.equals("")?"":("?" + params));
        
		SessionDao.instance.addModel_CSRFtoken(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_LOGO_URL(model);
		
		model.addAttribute("redirect", ssolwaReDirectURI);
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_CSRFtoken(model);
		
		return "action";
		
    }

    @RequestMapping("/{provider}/login")
    public String login( @PathVariable("provider") String provider, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        if(controller_map.containsKey(provider)){
        	return controller_map.get(provider).login(httpRequest, httpResponse, model);
        }
    	return toSSOLWALogin(httpRequest, model);
    }

    @RequestMapping("/{provider}/auth")
    public String authenticate(@PathVariable("provider") String provider, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
    	if(controller_map.containsKey(provider)){
    		return controller_map.get(provider).authenticate(httpRequest, httpResponse, model);
        }
    	return toSSOLWALogin(httpRequest, model);
    }

    @RequestMapping("/{provider}/basicinfo_confirm")
    public String confirmBasicInfo(@PathVariable("provider") String provider, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
        
        if(controller_map.containsKey(provider)){
        	return controller_map.get(provider).confirmBasicInfo(httpRequest, httpResponse, model);
        }
    	return toSSOLWALogin(httpRequest, model);
    }

    @RequestMapping("/{provider}/credential_confirm")
    public String confirmExist(@PathVariable("provider") String provider, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable {
       
        if(controller_map.containsKey(provider)){
        	return controller_map.get(provider).confirmExist(httpRequest, httpResponse, model);
        }
    	return toSSOLWALogin(httpRequest, model);
    }

}