package net.whydah.sso.extensions.crmcustomer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.CRMHelper;
import net.whydah.sso.dao.ConstantValue;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.errorhandling.AppException;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.extensions.crmcustomer.types.DeliveryAddress;
import net.whydah.sso.extensions.crmcustomer.types.EmailAddress;
import net.whydah.sso.extensions.crmcustomer.types.PhoneNumber;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.SSLTool;
import net.whydah.sso.utils.EscapeUtils;

@Controller
public class CRMCustomerController {

    private static final Logger log = LoggerFactory.getLogger(CRMCustomerController.class);

//    private final WhydahServiceClient tokenServiceClient = new WhydahServiceClient();
//    private URI crmServiceUri;
//    private final String emailVerificationLink;
//    String LOGOURL = "/sso/images/site-logo.png";

    public CRMCustomerController() throws IOException {
//        Properties properties = AppConfig.readProperties();
//        LOGOURL = properties.getProperty("logourl");
//        crmServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("crmservice")).build();
//        emailVerificationLink = AppConfig.readProperties().getProperty("email.verification.link");
    }

    //HUYD - refactor
//    @RequestMapping(value = "/crmdata", method = RequestMethod.POST)
//    public String createCrmdata(HttpServletRequest request, Model model) throws AppException {
//        log.trace("updateCrmdata was called");
////        model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
//        SessionDao.instance.addModel_LOGO_URL(model);
//        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
//        log.info("CRMCustomer - looking for userTokenId in Cookie, found {}", userTokenId);
//        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
//        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
//        log.info("CRMCustomer - looking for personRef found {}", personRef);
//
//        if (personRef != null) {
//            return updateCrmdata(request, model);
//        }
//        Customer customer = createCustomerFromRequest(request);
//
// //       String completeCustomerJson = CustomerMapper.toJson(customer);
//        
//        SessionDao.instance.getCRMHelper().createCrmData_AaddToModel(model, userTokenXml, customer);
//        
//
////        log.warn("FOR TESTING ONLY POST");
////        SSLTool.disableCertificateValidation();
////        personRef = new CommandCreateCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef, completeCustomerJson).execute();
//
//        model.addAttribute(ConstantValue.PERSON_REF, personRef);
//        model.addAttribute(ConstantValue.CRMCUSTOMER, customer);
//
//        return "editdata";
//    }
    
    @RequestMapping(value = "/crmdata", method = RequestMethod.POST)
    public String createCrmdata(HttpServletRequest request, HttpServletResponse response, Model model) throws AppException, JsonProcessingException {
        log.trace("createCrmdata was called");
       
        SessionDao.instance.addModel_LOGO_URL(model);
        //create customer data from request, with reference id = usertoke.getPersonRef()
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        Customer customer = CRMHelper.createCustomerFromRequest(request);
        SessionDao.instance.getCRMHelper().createCrmData_AaddToModel(model, userTokenXml, customer);
        //sync with UserToken
        syncFromCRMServiceToUAS(userTokenXml, customer);
		model.addAttribute(ConstantValue.USERTOKEN, trim(userTokenXml));
        return "editdata";
    }

    private void syncFromCRMServiceToUAS(String userTokenXml, Customer customer) {
        boolean shouldUpdate = false;
        UserToken ut = UserTokenMapper.fromUserTokenXml(userTokenXml);
        if (!ut.getFirstName().equals(customer.getFirstname())) {
            ut.setFirstName(customer.getFirstname());
            shouldUpdate = true;
        }
        if (!ut.getLastName().equals(customer.getLastname())) {
            ut.setLastName(customer.getLastname());
            shouldUpdate = true;
        }
        String email = customer.getEmailaddresses().get(customer.getDefaultEmailLabel()).getEmailaddress();
        if (!ut.getEmail().toLowerCase().equals(email.toLowerCase()) && email != null && !email.equals("")) {
            ut.setEmail(email);
            shouldUpdate = true;
        }
        String phone = customer.getPhonenumbers().get(customer.getDefaultPhoneLabel()).getPhonenumber();
        if (!ut.getCellPhone().equals(phone) && phone != null && !phone.equals("")) {
            ut.setCellPhone(phone);
            shouldUpdate = true;
        }

        if (shouldUpdate) {

            String result = SessionDao.instance.updateUserToken(ut);
            if (result == null) {
                log.error("Failed to sync this user to UAS");
            } else {
                log.info("User info has been updated to UAS successfully");
            }

        }
    }
    private Customer createCustomerFromRequest(HttpServletRequest request) throws AppException {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        log.info("CRMCustomer - looking for userTokenId in Cookie, found {}", userTokenId);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        String uid = UserTokenXpathHelper.getUserID(userTokenXml);

        //String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, UserTokenXpathHelper.getPersonref(userTokenXml)).execute();
        String crmCustomerJson = EscapeUtils.escapeHtml(SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml));
        Customer existingCustomer = null;
        if (crmCustomerJson != null) {
            existingCustomer = CustomerMapper.fromJson(crmCustomerJson);
        }


        Customer customer = new Customer();
        customer.setId(uid);
        customer.setEmailaddresses(new HashMap<String, EmailAddress>());
        customer.setPhonenumbers(new HashMap<String, PhoneNumber>());
        customer.setDeliveryaddresses(new HashMap<String, DeliveryAddress>());

        customer.setFirstname(request.getParameter("firstname"));
        customer.setMiddlename(request.getParameter("middlename"));
        customer.setLastname(request.getParameter("lastname"));


        String[] emailLabels = request.getParameterValues("emailLabel");
        String defaultEmail = request.getParameter("defaultEmail");
        customer.setDefaultEmailLabel(defaultEmail);

        for (int i = 0; i < emailLabels.length; i++) {
            String label = emailLabels[i];
            String emailAddress = request.getParameter(label + "_email");

            EmailAddress email = new EmailAddress(emailAddress, null, isEmailVerified(existingCustomer, label));
            customer.getEmailaddresses().put(label, email);
        }

        String[] phoneLabels = request.getParameterValues("phoneLabel");
        String defaultPhone = request.getParameter("defaultphone");
        customer.setDefaultPhoneLabel(defaultPhone);
        for (int i = 0; i < phoneLabels.length; i++) {
            String label = phoneLabels[i];
            String phone = request.getParameter(label + "_phone");
            PhoneNumber phoneNumber = new PhoneNumber(phone, null, isPhoneVerified(existingCustomer, label));
            customer.getPhonenumbers().put(label, phoneNumber);
        }


        String[] addressLabels = request.getParameterValues("addressLabel");
        String defaultAddress = request.getParameter("defaultAddress");
        customer.setDefaultAddressLabel(defaultAddress);
        for (int i = 0; i < addressLabels.length; i++) {
            String addressline1 = request.getParameter(addressLabels[i] + "_addressLine1");
            String addressline2 = request.getParameter(addressLabels[i] + "_addressLine2");
            String postalcode = request.getParameter(addressLabels[i] + "_postalCode");
            String postalcity = request.getParameter(addressLabels[i] + "_postalCity");
            DeliveryAddress address = new DeliveryAddress(addressline1, addressline2, postalcode, postalcity);
            customer.getDeliveryaddresses().put(addressLabels[i], address);
        }
        return customer;
    }

    private boolean isEmailVerified(Customer existingCustomer, String label) {
        boolean verified = false;
        if (existingCustomer != null) {
            if (existingCustomer.getEmailaddresses() != null && existingCustomer.getEmailaddresses().containsKey(label)) {
                verified = existingCustomer.getEmailaddresses().get(label).isVerified();
            }
        }
        return verified;
    }

    private boolean isPhoneVerified(Customer existingCustomer, String label) {
        boolean verified = false;
        if (existingCustomer != null) {
            if (existingCustomer.getPhonenumbers() != null && existingCustomer.getPhonenumbers().containsKey(label)) {
                verified = existingCustomer.getPhonenumbers().get(label).isVerified();
            }
        }
        return verified;
    }

    @RequestMapping(value = "/crmdata", method = RequestMethod.PUT)
    public String updateCrmdata(HttpServletRequest request, Model model) throws AppException {
    	  log.trace("updateCrmdata was called");
          SessionDao.instance.addModel_LOGO_URL(model);
          //update customer data from request, with reference id = usertoke.getPersonRef()
          String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
          String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
          String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
          Customer customer = CRMHelper.createCustomerFromRequest(request);
          String completeCustomerJson = INNCRMCustomerMapper.toJson(customer);
          SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, completeCustomerJson);
          //sync with UserToken
          syncFromCRMServiceToUAS(userTokenXml, customer);
          model.addAttribute(ConstantValue.PERSON_REF, personRef);
          model.addAttribute(ConstantValue.CRMCUSTOMER, Pattern.compile("\\\\").matcher(completeCustomerJson).replaceAll(""));
          model.addAttribute(ConstantValue.USERTOKEN, trim(userTokenXml));
          return "editdata";
    	
    }


    @RequestMapping(value = "/crmdata", method = RequestMethod.GET)
    public String getCrmdata(HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("getCrmdata was called");
        SessionDao.instance.addModel_LOGO_URL(model);
        String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
        SSLTool.disableCertificateValidation();
        String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
        crmCustomerJson = syncFromUAStoCRMService(userTokenXml, crmCustomerJson);
        model.addAttribute(ConstantValue.PERSON_REF, personRef);
        model.addAttribute(ConstantValue.CRMCUSTOMER, crmCustomerJson);
        model.addAttribute(ConstantValue.USERTOKEN, trim(userTokenXml));
        return "editdata";
    }
    
    private String syncFromUAStoCRMService(String userTokenXml, String crmCustomerJson) throws AppException {
        if (crmCustomerJson == null || JsonPathHelper.getStringFromJsonpathArrayExpression(crmCustomerJson, "$.id") == null) {
            log.warn("Called with dummy crmCustomerJson:{}", crmCustomerJson);
            return crmCustomerJson;
        }
        Customer customer = INNCRMCustomerMapper.fromJson(crmCustomerJson);
        //update this customer if usertoken info is different
        boolean shouldUpdate = false;
        UserToken ut = UserTokenMapper.fromUserTokenXml(userTokenXml);
        if (!ut.getFirstName().equals(customer.getFirstname())) {
            customer.setFirstname(ut.getFirstName());
            shouldUpdate = true;
        }
        if (!ut.getLastName().equals(customer.getLastname())) {
            customer.setLastname(ut.getLastName());
            shouldUpdate = true;
        }
        String email = customer.getEmailaddresses().get(customer.getDefaultEmailLabel()).getEmailaddress();
        if (!ut.getEmail().toLowerCase().equals(email.toLowerCase()) && email != null && !email.equals("")) {
            customer.getEmailaddresses().get(customer.getDefaultEmailLabel()).setEmailaddress(ut.getEmail());
            shouldUpdate = true;
        }
        String phone = customer.getPhonenumbers().get(customer.getDefaultPhoneLabel()).getPhonenumber();
        if (!ut.getCellPhone().equals(phone) && phone != null && !phone.equals("")) {
            customer.getPhonenumbers().get(customer.getDefaultPhoneLabel()).setPhonenumber(ut.getCellPhone());
            shouldUpdate = true;
        }

        //update crmservice because the info has been changed in usertoken
        if (shouldUpdate) {
        	 
            log.info("Changes detected in UserToken. Keep the customer record up to date in crmservice.");
            crmCustomerJson = INNCRMCustomerMapper.toInnJson(customer);
            String result = SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, crmCustomerJson);
            if (result == null) {
                log.error("Failed to update one customer record in crmservice");
            } else {
                log.info("Customer info has been updated in crmservice successfully");
            }
        }
        return crmCustomerJson;
    }

    //HUY- migrate from OID
//    @RequestMapping(value = "/crmdata", method = RequestMethod.GET)
//    public String getCrmdata(HttpServletRequest request, Model model) {
//        log.trace("getCrmdata was called");
//
//        //model.addAttribute(SessionHelper.LOGO_URL, LOGOURL);
//        SessionDao.instance.addModel_LOGO_URL(model);
//
//        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
//        try {
//            log.info("CRMCustomer - looking for userTokenId in Cookie, found {}", userTokenId);
//            String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
//            String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
//            log.info("CRMCustomer - looking personRef, found {}", personRef);
//
//            log.warn("FOR TESTING ONLY GET");
//            SSLTool.disableCertificateValidation();
//            if (personRef == null || personRef.isEmpty()) {
//                personRef = "1234";
//            }
//
//            //CRMHelper.addCrmdataToModel(crmServiceUri, tokenServiceClient.getMyAppTokenID(), model, userTokenXml);
//            /**
//             String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef).execute();
//
//             if (crmCustomerJson != null) {
//             Customer customer = CustomerMapper.fromJson(crmCustomerJson);
//
//             model.addAttribute(ModelHelper.PERSON_REF, personRef);
//             model.addAttribute(ModelHelper.CRMCUSTOMER, customer);
//             model.addAttribute(ModelHelper.USERTOKEN, trim(userTokenXml));
//             }
//             */
//            
//            String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml); 
//            if (crmCustomerJson != null) {
//                Customer customer = CustomerMapper.fromJson(crmCustomerJson);
//
//                model.addAttribute(ConstantValue.PERSON_REF, personRef);
//                model.addAttribute(ConstantValue.CRMCUSTOMER, customer);
//                model.addAttribute(ConstantValue.USERTOKEN, trim(userTokenXml));
//            }
//            
//            return "editdata";
//        } catch (Exception e) {
//            return "welcome";
//        }
//    }

    //HUY - refactor
//    @RequestMapping(value = "/profile", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
//    public String setJsonCrmdata(HttpServletRequest request, Model model) throws IOException {
//
//        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
//        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
//        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
//
//        ObjectMapper mapper = new ObjectMapper();
//        Customer customer = mapper.readValue(request.getReader(), Customer.class);
//
//        String customerJson = EscapeUtils.escapeHtml(CustomerMapper.toJson(customer));
//
//        String location = SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, customerJson);
//
//
//        model.addAttribute(ConstantValue.JSON_DATA, EscapeUtils.escapeHtml(customerJson));
//
//        return "json";
//    }
    
    @RequestMapping(value = "/profile", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public String setJsonCrmdata(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {

        String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
        StringBuffer postedJsonString = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                postedJsonString.append(line);
        } catch (Exception e) {
            log.error("Unable to understand updated Crmdata");
            /*report an error*/
        }
        log.trace("setJsonCrmdata - postedJsonString: {}", postedJsonString);
        Customer customer = INNCRMCustomerMapper.fromJson(postedJsonString.toString());
        String customerJson = INNCRMCustomerMapper.toJson(customer).replace('`', '"').replace('\t', ' ');
        customerJson = CRMHelper.fixEmailVerificationStatus(customerJson);
        log.trace("setJsonCrmdata - customerJson: {}", customerJson);
        String resultOfUpdate = SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, customerJson);
        
        syncFromCRMServiceToUAS(userTokenXml, customer);
        log.trace("setJsonCrmdata - customerJson: {}, resultOfUpdate: {}", customerJson, resultOfUpdate);
        model.addAttribute(ConstantValue.JSON_DATA, Pattern.compile("\\\\").matcher(customerJson).replaceAll(""));
        return "json";
    }



    @RequestMapping(value = "/profile", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getJsonCrmdata(HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {

//        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
//        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
//        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
//
//        String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
//
//        model.addAttribute(ConstantValue.JSON_DATA, "" + EscapeUtils.escapeHtml(crmCustomerJson));
//
//        return "json";
    	

    	String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
    	String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
    	crmCustomerJson = syncFromUAStoCRMService(userTokenXml, crmCustomerJson);
    	model.addAttribute(ConstantValue.JSON_DATA, crmCustomerJson);
    	return "json";
    }


    @RequestMapping(value = "/verify/phone_send_pin", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String sendPhoneVerificationPin(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String phoneNo = request.getParameter("phoneNo");

        boolean pinSent = SessionDao.instance.getCRMHelper().SendPhoneVerificationPin(userTokenXml, phoneNo);

        model.addAttribute(ConstantValue.JSON_DATA, ""+pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/phone_by_pin", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String verifyPhoneByPin(HttpServletRequest request, Model model) {

    	String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String phoneNo = request.getParameter("phoneNo");
        String pin = request.getParameter("pin");

        //Boolean pinSent = new CommandVerifyPhoneByPin(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, phoneNo, pin).execute();
        //use this
        boolean pinSent =  SessionDao.instance.getCRMHelper().SendPhoneVerificationPin(userTokenXml, phoneNo, pin);
        
        model.addAttribute(ConstantValue.JSON_DATA, ""+pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/email_send_token", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON)
    public String sendEmailVerificationToken(HttpServletRequest request, Model model) {

    	String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        //String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String email = SessionDao.instance.getFromRequest_Email(request);

        //Boolean tokenSent = new CommandSendEmailVerificationToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, email, emailVerificationLink).execute();
        //use this 
        boolean tokenSent = SessionDao.instance.getCRMHelper().sendEmailVerificiationToken(userTokenXml, email);
        
        model.addAttribute(ConstantValue.JSON_DATA, ""+tokenSent);

        return "json";
    }


    @RequestMapping(value = "/verify/email_by_token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String verifyEmailByToken(HttpServletRequest request, Model model) {
//
//        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
//        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
//        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);
//
//        String email = SessionDao.instance.getFromRequest_Email(request);
//        String token = SessionDao.instance.getFromRequest_Token(request);
//
//        //Boolean tokenVerified = new CommandVerifyEmailByToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, email, token).execute();
//        //use this instead
//        boolean tokenVerified = SessionDao.instance.getCRMHelper().verifyEmailByToken(userTokenXml, email, token);
//        model.addAttribute(ConstantValue.JSON_DATA, ""+tokenVerified);
//
//        return "json";
    	
    	String userticket = SessionDao.instance.getFromRequest_UserTicket(request);
        String email = SessionDao.instance.getFromRequest_Email(request);
        String emailtoken = SessionDao.instance.getFromRequest_Token(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTicket(userticket);

        //Boolean tokenVerified = new CommandVerifyEmailByToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, email, token).execute();
        //use this instead
        boolean tokenVerified = SessionDao.instance.getCRMHelper().verifyEmailByToken(userTokenXml, email, emailtoken);
        /*if (tokenVerified) {
            String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
            if (crmCustomerJson != null) {

                crmCustomerJson = SessionDao.instance.getCRMHelper().updateEmailVerificationStatus(crmCustomerJson, new String[]{email});
                //Update to CRM
                SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, crmCustomerJson);
            }
        }*/
        model.addAttribute(ConstantValue.JSON_DATA, "" + tokenVerified);
        String redirectURI = "login";
        try {
            redirectURI = AppConfig.readProperties().getProperty("email.verification.redirect.link");
            String newTicket = UUID.randomUUID().toString();
            boolean result = SessionDao.instance.getServiceClient().createTicketForUserTokenID(newTicket, UserTokenMapper.fromUserTokenXml(userTokenXml).getUserTokenId());
            if(result) {
            	redirectURI = redirectURI + (redirectURI.contains("&")?"&" :"?") + "userticket=" + newTicket;
            }
        } catch (IOException ioe) {
            log.warn("Error when reading 'email.verification.redirect.link' from config parameters", ioe);
        }
        model.addAttribute(ConstantValue.REDIRECT, redirectURI);
        model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
        log.info("login - Redirecting to {}", redirectURI);
        return "action";
    }


    @RequestMapping(value = "/profilemock", method = RequestMethod.PUT)
    public String setMockJsonCrmdata(HttpServletRequest request, Model model) throws IOException {

        log.warn("Mock service: Parsing and returning data.");
        ObjectMapper mapper = new ObjectMapper();
        Customer customer = mapper.readValue(request.getReader(), Customer.class);

        model.addAttribute(ConstantValue.JSON_DATA, CustomerMapper.toJson(customer));

        return "json";
    }


    @RequestMapping(value = "/profilemock", method = RequestMethod.GET)
    public String getMockJsonCrmdata(HttpServletRequest request, Model model) {
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);

        log.warn("Mock service: Returning mock-data only");
        Customer customer = createMockCustomer(userTokenId);

        model.addAttribute(ConstantValue.JSON_DATA, CustomerMapper.toJson(customer));

        return "json";
    }

    private Customer createMockCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setEmailaddresses(new HashMap<String, EmailAddress>());
        customer.setPhonenumbers(new HashMap<String, PhoneNumber>());
        customer.setDeliveryaddresses(new HashMap<String, DeliveryAddress>());

        customer.setFirstname("Capra");
        customer.setMiddlename(null);
        customer.setLastname("Consulting");
        customer.setSex("M");
        customer.setBirthdate(new java.util.Date(0));

        customer.setDefaultEmailLabel("home");
        customer.getEmailaddresses().put("home", new EmailAddress("private.mail@hotmail.com", null, false));
        customer.getEmailaddresses().put("work", new EmailAddress("mail@capraconsulting.no", null, true));

        customer.setDefaultPhoneLabel("mobile");

        customer.getPhonenumbers().put("mobile", new PhoneNumber("44556677", null, true));
        customer.getPhonenumbers().put("work-mobile", new PhoneNumber("98765432", null, false));
        customer.getPhonenumbers().put("home-mobile", new PhoneNumber("43215678", null, true));

        customer.setDefaultAddressLabel("home");
        customer.getDeliveryaddresses().put("home", new DeliveryAddress("Karl Johans gate 6", null, "0154", "Oslo"));
        customer.getDeliveryaddresses().put("work", new DeliveryAddress("Stenersgaten 2", null, "0184", "Oslo"));

        return customer;
    }

    public static String trim(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @RequestMapping(value = "/verify/phone_send_pin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON)
    public String sendPhoneVerificationPin(HttpServletRequest request, HttpServletResponse response, Model model) {

        //String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        //String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        //String phoneNo = request.getParameter("phoneNo");
        //use this
        String phoneNo = SessionDao.instance.getFromRequest_CellPhone(request);

        //Boolean pinSent = new CommandSendPhoneVerificationPin(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, phoneNo).execute();
        //use this
        boolean pinSent = SessionDao.instance.getCRMHelper().SendPhoneVerificationPin(userTokenXml, phoneNo);

        model.addAttribute(ConstantValue.JSON_DATA, "" + pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/phone_by_pin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON)
    public String verifyPhoneByPin(HttpServletRequest request, HttpServletResponse response, Model model) {

        //String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        //String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);

        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String phoneNo = request.getParameter("phoneNo");
        String pin = request.getParameter("pin");

        //Boolean pinSent = new CommandVerifyPhoneByPin(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, phoneNo, pin).execute();
        //use this
        boolean pinSent = SessionDao.instance.getCRMHelper().SendPhoneVerificationPin(userTokenXml, phoneNo, pin);

        model.addAttribute(ConstantValue.JSON_DATA, "" + pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/email_send_token", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public String sendEmailVerificationToken(HttpServletRequest request, HttpServletResponse response, Model model) {

        String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);

        String email = SessionDao.instance.getFromJsonRequest_Email(request);

        //Boolean tokenSent = new CommandSendEmailVerificationToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, email, emailVerificationLink).execute();
        //use this 
        boolean tokenSent = SessionDao.instance.getCRMHelper().sendEmailVerificiationToken(userTokenXml, email);

        model.addAttribute(ConstantValue.JSON_DATA, "" + tokenSent);

        return "json";
    }

    /**
     * Using HTTP GET here as it is a simple mail-link
     */
    @RequestMapping(value = "/verify/email_by_token", method = RequestMethod.GET)
    public String verifyEmailByToken(HttpServletRequest request, HttpServletResponse response, Model model) {

        //String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        //String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
        //String userTokenXml = SessionDao.instance.findUserTokenXMLFromSession(request, response, model);
        //String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String userticket = SessionDao.instance.getFromRequest_UserTicket(request);
        String email = SessionDao.instance.getFromRequest_Email(request);
        String emailtoken = SessionDao.instance.getFromRequest_Token(request);
        String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTicket(userticket);

        //Boolean tokenVerified = new CommandVerifyEmailByToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userTokenId, personRef, email, token).execute();
        //use this instead
        boolean tokenVerified = SessionDao.instance.getCRMHelper().verifyEmailByToken(userTokenXml, email, emailtoken);
//        if (tokenVerified) {
//            String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
//            if (crmCustomerJson != null) {
//
//                crmCustomerJson = CRMHelper.updateEmailVerificationStatus(crmCustomerJson, new String[]{email});
//                //Update to CRM
//                SessionDao.instance.getCRMHelper().updateCrmData(userTokenXml, crmCustomerJson);
//            }
//        }
        model.addAttribute(ConstantValue.JSON_DATA, "" + tokenVerified);
        String redirectURI = "login";
        try {
            redirectURI = AppConfig.readProperties().getProperty("email.verification.redirect.link");
            String newTicket = UUID.randomUUID().toString();
            boolean result = SessionDao.instance.getServiceClient().createTicketForUserTokenID(newTicket, UserTokenMapper.fromUserTokenXml(userTokenXml).getUserTokenId());
            if(result) {
            	redirectURI = redirectURI + (redirectURI.contains("&")?"&" :"?") + "userticket=" + newTicket;
            }
        } catch (IOException ioe) {
            log.warn("Error when reading 'email.verification.redirect.link' from config parameters", ioe);
        }
        model.addAttribute(ConstantValue.REDIRECT, redirectURI);
        model.addAttribute(ConstantValue.REDIRECT_URI, redirectURI);
        log.info("login - Redirecting to {}", redirectURI);
        return "action";

    }

}
