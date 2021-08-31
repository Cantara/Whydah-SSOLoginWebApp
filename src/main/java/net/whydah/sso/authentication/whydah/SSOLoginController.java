package net.whydah.sso.authentication.whydah;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.ddd.model.user.UserTokenId;
import net.whydah.sso.errorhandling.AppException;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.utils.SignupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Controller
public class SSOLoginController {

    private final static Logger log = LoggerFactory.getLogger(SSOLoginController.class);

    public SSOLoginController() throws IOException {

    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response,Model model) {
    	
    	
    	String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
    	boolean sessionCheckOnly = SessionDao.instance.getFromRequest_SessionCheck(request);
		
	
		SessionDao.instance.updateApplinks();
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);
        SessionDao.instance.addModel_MYURI(model);
        SessionDao.instance.addModel_WHYDAH_VERSION(model);
        model.addAttribute(ConstantValue.SESSIONCHECK, sessionCheckOnly);
        model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
        
        CookieManager.addSecurityHTTPHeaders(response);

        //usertokenId = cookieManager.getUserTokenIdFromCookie(request, response);
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        log.trace("login: redirectURI={}, SessionCheck={}, userTokenIdFromCookie={}", redirectURI, sessionCheckOnly, userTokenIdFromCookie);

        UserTokenId whydahUserTokenId = null;
        if ("logout".equalsIgnoreCase(userTokenIdFromCookie)) {
            log.info("userTokenId={} from cookie. TODO: should probably clear the logout cookie here?", userTokenIdFromCookie);
            CookieManager.clearUserTokenCookies(request, response);
            //usertokenId = WhydahUserTokenId.invalidTokenId();
        } else if (userTokenIdFromCookie != null && SessionDao.instance.getServiceClient().verifyUserTokenId(userTokenIdFromCookie)) {
            log.trace("userTokenId={} from cookie verified OK.", userTokenIdFromCookie);
            whydahUserTokenId = new UserTokenId(userTokenIdFromCookie);
        } else {
            CookieManager.clearUserTokenCookies(request, response);
        }

        if (whydahUserTokenId != null) {
            log.trace("login - whydahUserTokenId={} is valid", whydahUserTokenId);

            if (ConstantValue.DEFAULT_REDIRECT.equalsIgnoreCase(redirectURI)) {
                log.trace("login - Did not find any sensible redirectURI, using /welcome");
                model.addAttribute(ConstantValue.REDIRECT, redirectURI);
                //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                SessionDao.instance.addModel_CSRFtoken(model);
                log.info("login - Redirecting to {}", redirectURI);
                return "action";

            }
            String userTicket = UUID.randomUUID().toString();
            if (SessionDao.instance.getServiceClient().createTicketForUserTokenID(userTicket, whydahUserTokenId.toString())){
                log.info("login - created new userticket={} for usertokenid={}",userTicket, whydahUserTokenId);
                redirectURI = SessionDao.instance.getServiceClient().appendTicketToRedirectURI(redirectURI, userTicket);

                // Action use redirect - not redirectURI
                model.addAttribute(ConstantValue.REDIRECT, redirectURI);
                log.info("login - Redirecting to {}", redirectURI);
                //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                SessionDao.instance.addModel_CSRFtoken(model);
                return "action";
            }
        }

        // Added return is sessioncheck only and no cookie found
        if (sessionCheckOnly) {
            // Action use redirect - not redirectURI
            model.addAttribute(ConstantValue.REDIRECT, redirectURI);
            log.info("login - isSessionCheckOnly - Redirecting to {}", redirectURI);
            return "action";

        } 

        //ModelHelper.setEnabledLoginTypes(model);
        SessionDao.instance.addModel_LoginTypes(model);
        //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        SessionDao.instance.addModel_CSRFtoken(model);
        return "login";
    }

    @RequestMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response,Model model) {
    	
    	String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
    	SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_WHYDAH_VERSION(model);
		SessionDao.instance.addModel_LoginTypes(model);
		SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);


        if (userTokenXml == null) {
            return "login";
		} else {

			boolean sessionCheckOnly = SessionDao.instance.getFromRequest_SessionCheck(request);
			model.addAttribute(ConstantValue.SESSIONCHECK, sessionCheckOnly);
			model.addAttribute(ConstantValue.USERTOKEN, SignupHelper.trim(userTokenXml));
			model.addAttribute(ConstantValue.APP_LINKS, SessionDao.instance.getAppLinks());
			model.addAttribute(ConstantValue.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
			model.addAttribute(ConstantValue.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
			model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
			model.addAttribute(ConstantValue.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
			model.addAttribute(ConstantValue.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
			//SessionDao.instance.getReportServiceHelper().addUserActivities(model, userTokenXml);
			return ConstantValue.DEFAULT_REDIRECT;
		}
    }

    @RequestMapping("/action")
    public String action(HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
    	
    	String redirectURI = SessionDao.instance.getFromRequest_RedirectURI(request);
		log.trace("action: redirectURI: {}", redirectURI);
		
		SessionDao.instance.addModel_LOGO_URL(model);
		SessionDao.instance.addModel_MYURI(model);
		SessionDao.instance.addModel_WHYDAH_VERSION(model);
		SessionDao.instance.addModel_IAM_MODE(model);
		SessionDao.instance.addModel_CSRFtoken(model);
        SessionDao.instance.setCSP(response);
        SessionDao.instance.addModel_LoginTypes(model);

        model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);

        SessionDao.instance.addModel_LoginTypes(model);
        //if (!SessionHelper.validCSRFToken(request.getParameter(SessionHelper.CSRFtoken))) {
		if(!SessionDao.instance.validCSRFToken(SessionDao.instance.getfromRequest_CSRFtoken(request))) {
            log.warn("action - CSRFtoken verification failed. Redirecting to login.");
            model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in - CSRFtoken missing or incorrect");
            //model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            SessionDao.instance.addModel_CSRFtoken(model);
            //ModelHelper.setEnabledLoginTypes(model);
            SessionDao.instance.addModel_LoginTypes(model);
            CookieManager.clearUserTokenCookies(request, response);
            model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
            return "login";

        }

        if (!UserName.isValid(SessionDao.instance.getFromRequest_User(request)) || !Password.isValid(SessionDao.instance.getFromRequest_Password(request))) {
            log.warn("action - illegal username or password. Redirecting to login.");
            model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in.");
            CookieManager.clearUserTokenCookies(request, response);
            return "login";
        }

        
        UserCredential user = new UserNameAndPasswordCredential(SessionDao.instance.getFromRequest_User(request), SessionDao.instance.getFromRequest_Password(request));
        String userTicket = UUID.randomUUID().toString();
        String userTokenXml = SessionDao.instance.getServiceClient().getUserToken(user, userTicket);

        if (userTokenXml == null) {
            log.warn("action - getUserToken failed. Redirecting to login.");
            model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not log in.");
            CookieManager.clearUserTokenCookies(request, response);
            return "login";
        }

        SessionDao.instance.getServiceClient().getWAS().updateDefcon(userTokenXml);
        if (redirectURI.contains(ConstantValue.USERTICKET) && !redirectURI.toLowerCase().contains("http")) {
            log.warn("action - redirectURI contain ticket and no URL. Redirecting to welcome.");
            model.addAttribute(ConstantValue.LOGIN_ERROR, "Could not redirect back, redirect loop detected.");
            //ModelHelper.setEnabledLoginTypes(model);
            SessionDao.instance.addModel_LoginTypes(model);
            model.addAttribute(ConstantValue.REDIRECT_URI, "");
            model.addAttribute(ConstantValue.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
            model.addAttribute(ConstantValue.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
            model.addAttribute(ConstantValue.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
            model.addAttribute(ConstantValue.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
            model.addAttribute(ConstantValue.DEFCON, SessionDao.instance.getServiceClient().getWAS().getDefcon());
            model.addAttribute(ConstantValue.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
            SessionDao.instance.getCRMHelper().getCrmdata_AddToModel(model, userTokenXml);
            //SessionDao.instance.getReportServiceHelper().addUserActivities(model, userTokenXml);
            return "welcome";
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

        // ticket on redirect
        if (redirectURI.toLowerCase().contains(ConstantValue.USERTICKET)) {
            // Do not overwrite ticket
        } else {
            redirectURI = SessionDao.instance.getServiceClient().appendTicketToRedirectURI(redirectURI, userTicket);
        }
        // Action use redirect...
        model.addAttribute(ConstantValue.REDIRECT, redirectURI);
        model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
        log.info("action - Redirecting to {}", redirectURI);
        return "action";
    }
    
}


