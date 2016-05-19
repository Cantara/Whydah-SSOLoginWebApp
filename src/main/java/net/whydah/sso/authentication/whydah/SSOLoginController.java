package net.whydah.sso.authentication.whydah;

import static net.whydah.sso.config.ModelHelper.INN_ROLE;
import net.whydah.sso.ServerRunner;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.UserCredential;
import net.whydah.sso.authentication.UserNameAndPasswordCredential;
import net.whydah.sso.authentication.whydah.clients.WhyDahServiceClient;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandGetUserRoles;
import net.whydah.sso.commands.adminapi.user.role.CommandUpdateUserRole;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.commands.extensions.statistics.CommandListUserActivities;
import net.whydah.sso.commands.userauth.CommandRefreshUserToken;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.config.SessionHelper;
import net.whydah.sso.extensions.useractivity.helpers.UserActivityHelper;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserRoleMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.user.types.UserTokenID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Controller
public class SSOLoginController {

    private final static Logger log = LoggerFactory.getLogger(SSOLoginController.class);
    private final WhyDahServiceClient tokenServiceClient;
    private String LOGOURL = "/sso/images/site-logo.png";
    private String whydahVersion = ServerRunner.version;

    private String crmservice;
    private String reportservice;
    private URI uasServiceUri;
	private URI tokenServiceUri;
	
    //private final int MIN_REDIRECT_SIZE=4;
    //private final ModelHelper modelHelper = new ModelHelper(this);


    public SSOLoginController() throws IOException {
        Properties properties = AppConfig.readProperties();
        LOGOURL = properties.getProperty("logourl");
        crmservice = properties.getProperty("crmservice");
        reportservice = properties.getProperty("reportservice");
        
        this.tokenServiceClient = new WhyDahServiceClient();
        this.tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
		this.uasServiceUri = UriBuilder.fromUri(properties.getProperty("useradminservice")).build();
		
    }


    @RequestMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response,Model model) {
        String redirectURI = SessionHelper.getRedirectURI(request);
        boolean sessionCheckOnly = isSessionCheckOnly(request);
        model.addAttribute(SessionHelper.SESSIONCHECK, sessionCheckOnly);

        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.WHYDAH_VERSION,whydahVersion);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);


        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        CookieManager.addSecurityHTTPHeaders(response);


        //usertokenId = cookieManager.getUserTokenIdFromCookie(request, response);
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        log.trace("login: redirectURI={}, SessionCheck={}, userTokenIdFromCookie={}", redirectURI, sessionCheckOnly, userTokenIdFromCookie);

        UserTokenID whydahUserTokenId = UserTokenID.invalidTokenID();
        if ("logout".equalsIgnoreCase(userTokenIdFromCookie)) {
            log.info("userTokenId={} from cookie. TODO: should probably clear the logout cookie here?", userTokenIdFromCookie);
            CookieManager.clearUserTokenCookies(request, response);
            //usertokenId = WhydahUserTokenId.invalidTokenId();
        } else if (userTokenIdFromCookie != null && tokenServiceClient.verifyUserTokenId(userTokenIdFromCookie)) {
            log.trace("userTokenId={} from cookie verified OK.", userTokenIdFromCookie);
            whydahUserTokenId = UserTokenID.fromUserTokenID(userTokenIdFromCookie);
        } else {
            CookieManager.clearUserTokenCookies(request, response);
        }

        if (whydahUserTokenId.isValid()) {
            log.trace("login - whydahUserTokenId={} is valid", whydahUserTokenId);

            if (SessionHelper.DEFAULT_REDIRECT.equalsIgnoreCase(redirectURI)) {
                log.trace("login - Did not find any sensible redirectURI, using /welcome");
                model.addAttribute(SessionHelper.REDIRECT, redirectURI);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                log.info("login - Redirecting to {}", redirectURI);
                return "action";

            }
            String userTicket = UUID.randomUUID().toString();
            if (tokenServiceClient.createTicketForUserTokenID(userTicket, whydahUserTokenId.toString())){
                log.info("login - created new userticket={} for usertokenid={}",userTicket, whydahUserTokenId);
                redirectURI = tokenServiceClient.appendTicketToRedirectURI(redirectURI, userTicket);

                // Action use redirect - not redirectURI
                model.addAttribute(SessionHelper.REDIRECT, redirectURI);
                log.info("login - Redirecting to {}", redirectURI);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                return "action";
            }
        }

        // Added return is sessioncheck only and no cookie found
        if (isSessionCheckOnly(request)) {
            // Action use redirect - not redirectURI
            model.addAttribute(SessionHelper.REDIRECT, redirectURI);
            log.info("login - isSessionCheckOnly - Redirecting to {}", redirectURI);
            return "action";

        } 

        ModelHelper.setEnabledLoginTypes(model);
        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        return "login";
    }



    @RequestMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response,Model model) {
        String userTicket = request.getParameter(ModelHelper.USERTICKET);
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = "";
        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.IAM_MODE, ApplicationMode.getApplicationMode());
        model.addAttribute(SessionHelper.WHYDAH_VERSION, whydahVersion);
        CookieManager.addSecurityHTTPHeaders(response);
        try {
            if (userTicket != null && userTicket.length() > 3) {
                log.trace("Welcome - Using userTicket");
                userTokenXml = tokenServiceClient.getUserTokenByUserTicket(userTicket);
                model.addAttribute(ModelHelper.USERTICKET, userTicket);
                model.addAttribute(ModelHelper.USERTOKEN, userTokenXml);
            } else if (userTokenId != null && userTokenId.length() > 3) {
                log.trace("Welcome - No userTicket, using userTokenID from cookie");
                userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
                model.addAttribute(ModelHelper.USERTICKET, "No userTicket, using userTokenID");
                model.addAttribute(ModelHelper.USERTOKEN, userTokenXml);
                model.addAttribute(ModelHelper.USER_TOKEN_ID, userTokenId);
            } else {
                // TODO cleanup - this messes up the log for a normal case
                //throw new UnauthorizedException();
                log.trace("Welcome - no session found");
                CookieManager.clearUserTokenCookies(request, response);
                ModelHelper.setEnabledLoginTypes(model);
                model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
                return "login";
            }
        } catch (Exception e){
            log.warn("welcome redirect - SecurityTokenException exception: ",e);
            CookieManager.clearUserTokenCookies(request, response);
            ModelHelper.setEnabledLoginTypes(model);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "login";
        }
        model.addAttribute(ModelHelper.USERTOKEN, trim(userTokenXml));
        model.addAttribute(SessionHelper.APP_LINKS, SessionHelper.getAppLinks());
        log.trace("embedded applinks: " + SessionHelper.getAppLinks());
        model.addAttribute(ModelHelper.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
        model.addAttribute(ModelHelper.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
        model.addAttribute(ModelHelper.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
        model.addAttribute(ModelHelper.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
        model.addAttribute(ModelHelper.DEFCON, UserTokenXpathHelper.getDEFCONLevel(userTokenXml));
        model.addAttribute(ModelHelper.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
        addCrmCustomer(model, userTokenXml);
        addUserActivities(model, userTokenXml);
        return "welcome";
    }


    @RequestMapping("/action")
    public String action(HttpServletRequest request, HttpServletResponse response, Model model) {
        String redirectURI = SessionHelper.getRedirectURI(request);
        log.trace("action: redirectURI: {}", redirectURI);
        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
        model.addAttribute(SessionHelper.WHYDAH_VERSION,whydahVersion);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);


        if (!SessionHelper.validCSRFToken(request.getParameter(SessionHelper.CSRFtoken))) {
            log.warn("action - CSRFtoken verification failed. Redirecting to login.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in - CSRFtoken missing or incorrect");
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            ModelHelper.setEnabledLoginTypes(model);
            CookieManager.clearUserTokenCookies(request, response);
            model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
            return "login";

        }
        UserCredential user = new UserNameAndPasswordCredential(request.getParameter(SessionHelper.USER), request.getParameter(SessionHelper.PASSWORD));
        String userTicket = UUID.randomUUID().toString();
        String userTokenXml = tokenServiceClient.getUserToken(user, userTicket);

        if (userTokenXml == null) {
            log.warn("action - getUserToken failed. Redirecting to login.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in.");
            ModelHelper.setEnabledLoginTypes(model);
            CookieManager.clearUserTokenCookies(request, response);
            model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
            return "login";
        }
        if (redirectURI.contains(ModelHelper.USERTICKET) && !redirectURI.toLowerCase().contains("http")) {
            log.warn("action - redirectURI contain ticket and no URL. Redirecting to welcome.");
            model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not redirect back, redirect loop detected.");
            ModelHelper.setEnabledLoginTypes(model);
            model.addAttribute(SessionHelper.REDIRECT_URI, "");
            model.addAttribute(ModelHelper.REALNAME, UserTokenXpathHelper.getRealName(userTokenXml));
            model.addAttribute(ModelHelper.PHONE_NUMBER, UserTokenXpathHelper.getPhoneNumber(userTokenXml));
            model.addAttribute(ModelHelper.SECURITY_LEVEL, UserTokenXpathHelper.getSecurityLevel(userTokenXml));
            model.addAttribute(ModelHelper.EMAIL, UserTokenXpathHelper.getEmail(userTokenXml));
            model.addAttribute(ModelHelper.DEFCON, UserTokenXpathHelper.getDEFCONLevel(userTokenXml));
            model.addAttribute(ModelHelper.PERSON_REF, UserTokenXpathHelper.getPersonref(userTokenXml));
            addCrmCustomer(model, userTokenXml);
            addUserActivities(model, userTokenXml);
            return "welcome";
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = WhyDahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

        // ticket on redirect
        if (redirectURI.toLowerCase().contains(SessionHelper.USERTICKET)) {
            // Do not overwrite ticket
        } else {
            redirectURI = tokenServiceClient.appendTicketToRedirectURI(redirectURI, userTicket);
        }
        // Action use redirect...
        model.addAttribute(SessionHelper.REDIRECT, redirectURI);
        model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
        model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
        log.info("action - Redirecting to {}", redirectURI);
        return "action";
    }

    private void addCrmCustomer(Model model, String userToken) {
        if (UserTokenXpathHelper.getPersonref(userToken).length() > 2) {
            try {
                String personRef = net.whydah.sso.user.helpers.UserTokenXpathHelper.getPersonref(userToken);
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userToken);
                String crmCustomerJson = new CommandGetCRMCustomer(URI.create(crmservice), tokenServiceClient.getMyAppTokenID(), userTokenId, personRef).execute();
                model.addAttribute(ModelHelper.CRMCUSTOMER, crmCustomerJson);
                model.addAttribute(ModelHelper.JSON_DATA, crmCustomerJson);
            } catch (Exception e) {

            }
        }
    }

    private void addUserActivities(Model model, String userTokenXml) {
        model.addAttribute(ModelHelper.USERACTIVITIES_SIMPLIFIED, "{}");
        if (false && UserTokenXpathHelper.getUserID(userTokenXml).length() > 2) {
            try {
                String userid = UserTokenXpathHelper.getUserID(userTokenXml);
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
                log.warn(">==================== 1 ");

                String userActivitiesJson = new CommandListUserActivities(URI.create(reportservice), tokenServiceClient.getMyAppTokenID(), userTokenId, userid).execute();
                //    model.addAttribute(ModelHelper.USERACTIVITIES, userActivitiesJson);
                log.warn(">==================== 2 ");
                model.addAttribute(ModelHelper.USERACTIVITIES_SIMPLIFIED, UserActivityHelper.getUserSessionsJsonFromUserActivityJson(userActivitiesJson, userid));
                log.warn(">==================== 3 ");
            } catch (Exception e) {

            }
        }
    }



    private boolean isSessionCheckOnly(HttpServletRequest request) {
        String redirectURI = request.getParameter(SessionHelper.SESSIONCHECK);
        if (redirectURI == null || redirectURI.length() < 1) {
            log.trace("isSessionCheckOnly - false - No SESSIONCHECK param found");
            return false;
        }
        return true;
    }


    public static String trim(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    
    @RequestMapping("/selectaddress")
	public String selectAddress(HttpServletRequest request, HttpServletResponse response, Model model) {
		//get parameters
		String redirectURI = SessionHelper.getRedirectURI(request);
		log.trace("selectaddress: redirectURI: {}", redirectURI);
		String CSRFtoken = request.getParameter(SessionHelper.CSRFtoken);//to check for valid request
		String userTicket = request.getParameter(ModelHelper.USERTICKET);//need a ticket to get UserToken
		String address = request.getParameter(SessionHelper.ADDRESS);//It is the roleValue of roleName=INNDATA
		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);//accept tokenid from cookie as well
		String appplicationId = request.getParameter(SessionHelper.APPLICATIONID);
		String appplicationName = request.getParameter(SessionHelper.APPLICATIONNAME);
		model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
		model.addAttribute(SessionHelper.IAM_MODE, ApplicationMode.getApplicationMode());
		model.addAttribute(SessionHelper.WHYDAH_VERSION, whydahVersion);
		model.addAttribute(SessionHelper.CSRFtoken, SessionHelper.getCSRFtoken());
		model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
		ModelHelper.setEnabledLoginTypes(model);

		String userTokenXml = null;
		ModelHelper.setEnabledLoginTypes(model);

		if (SessionHelper.validCSRFToken(CSRFtoken)) {
			if (userTicket != null && userTicket.length() > 3) {
				log.trace("selectaddress - Using userTicket");
				userTokenXml = tokenServiceClient.getUserTokenByUserTicket(userTicket);
			} else if (userTokenId != null && userTokenId.length() > 3) {
				log.trace("selectaddress - No userTicket, using userTokenID from cookie");
				userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
			} else {
				String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
				log.trace("selectaddress - No userTicket, using userTokenID from cookie");
				userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenIdFromCookie);
			}

			//Process when having a valid UserToken
			if (userTokenXml != null) {
				if (tokenServiceClient.updateOrCreateUserApplicationRoleEntry(appplicationId, appplicationName, "WHYDAH", INN_ROLE, address, userTokenXml)) {
					if (true) { 

						model.addAttribute(SessionHelper.REDIRECT, redirectURI);
						model.addAttribute(SessionHelper.REDIRECT_URI, redirectURI);
						return "action";
					}
				} else {
					model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in - Update Application Role Failed");
				}
			}

		} else {
			log.warn("action - CSRFtoken verification failed. Redirecting to login.");
			model.addAttribute(SessionHelper.LOGIN_ERROR, "Could not log in - CSRFtoken missing or incorrect");
		}


		log.trace("Select address - no session found");
		ModelHelper.setEnabledLoginTypes(model);
		return "login";


	}

	private String getCellPhone(HttpServletRequest request) {
		String cellPhone = request.getParameter(SessionHelper.CELL_PHONE);
		if (cellPhone == null || cellPhone.length() < 1) {
			log.trace("getCellPhone - No cellphone found, setting to ", "");
			return "";
		}
		return cellPhone;
	}
	

}


