package net.whydah.sso.authentication;

import net.whydah.sso.dao.SessionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SSORootController {

    private final static Logger log = LoggerFactory.getLogger(SSORootController.class);
    RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    
    @RequestMapping("/")
    public void mainPage(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {
    	redirectStrategy.sendRedirect(request, response, "/login" + (request.getQueryString()!=null? ("?" + request.getQueryString()):"") );
    }
    
}
