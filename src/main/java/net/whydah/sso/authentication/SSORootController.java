package net.whydah.sso.authentication;

import net.whydah.sso.dao.SessionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SSORootController {

    private final static Logger log = LoggerFactory.getLogger(SSORootController.class);

    @RequestMapping("/")
    public String mainPage(HttpServletRequest request, HttpServletResponse response, Model model) {
        log.info("Resolved mainPage - returning login");
        SessionDao.instance.addModel_LOGO_URL(model);
        SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);
        SessionDao.instance.addModel_LoginTypes(model);


        SessionDao.instance.addModel_MYURI(model);
        SessionDao.instance.addModel_WHYDAH_VERSION(model);

        return "login";
    }

}
