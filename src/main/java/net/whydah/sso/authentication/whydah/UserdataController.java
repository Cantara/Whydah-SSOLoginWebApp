package net.whydah.sso.authentication.whydah;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.commands.extensions.crmapi.*;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.extensions.crmcustomer.types.DeliveryAddress;
import net.whydah.sso.extensions.crmcustomer.types.EmailAddress;
import net.whydah.sso.extensions.crmcustomer.types.PhoneNumber;
import net.whydah.sso.authentication.whydah.clients.SecurityTokenServiceClient;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.util.SSLTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Properties;

@Controller
public class UserdataController {

    private static final Logger log = LoggerFactory.getLogger(UserdataController.class);

    private final SecurityTokenServiceClient tokenServiceClient = new SecurityTokenServiceClient();
    private URI crmServiceUri;
    private final String emailVerificationLink;
    String LOGOURL = "/sso/images/site-logo.png";

    public UserdataController() throws IOException {
        Properties properties = AppConfig.readProperties();
        LOGOURL = properties.getProperty("logourl");
        crmServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("crmservice")).build();
        emailVerificationLink = AppConfig.readProperties().getProperty("email.verification.link");
    }

    @RequestMapping(value = "/userdata", method = RequestMethod.POST)
    public String createCrmdata(HttpServletRequest request, Model model) {
        log.trace("updateCrmdata was called");
        model.addAttribute("logoURL", LOGOURL);

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        if (personRef != null) {
            return updateCrmdata(request, model);
        }
        Customer customer = createCustomerFromRequest(request);

        String completeCustomerJson = CustomerMapper.toJson(customer);

        log.warn("FOR TESTING ONLY");
        SSLTool.disableCertificateValidation();
        personRef = new CommandCreateCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef, completeCustomerJson).execute();

        model.addAttribute(ModelHelper.PERSON_REF, personRef);
        model.addAttribute(ModelHelper.CRMCUSTOMER, customer);

        return "editdata";
    }

    private Customer createCustomerFromRequest(HttpServletRequest request) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String uid = UserTokenXpathHelper.getUserID(userTokenXml);

        String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, UserTokenXpathHelper.getPersonref(userTokenXml)).execute();
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
            String emailAddress = request.getParameter(label +"_email");

            EmailAddress email = new EmailAddress(emailAddress, null, isEmailVerified(existingCustomer, label));
            customer.getEmailaddresses().put(label, email);
        }

        String[] phoneLabels = request.getParameterValues("phoneLabel");
        String defaultPhone = request.getParameter("defaultphone");
        customer.setDefaultPhoneLabel(defaultPhone);
        for (int i = 0; i < phoneLabels.length; i++) {
            String label = phoneLabels[i];
            String phone = request.getParameter(label + "_phone");
            PhoneNumber phoneNumber = new PhoneNumber(phone, null,  isPhoneVerified(existingCustomer, label));
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
    public String updateCrmdata(HttpServletRequest request, Model model) {
        log.trace("updateCrmdata was called");
        model.addAttribute("logoURL", LOGOURL);

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        if (personRef == null) {
            return createCrmdata(request, model);
        }
        Customer customer = createCustomerFromRequest(request);
        String completeCustomerJson = CustomerMapper.toJson(customer);

        log.warn("FOR TESTING ONLY");
        SSLTool.disableCertificateValidation();
        String location = new CommandUpdateCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef, completeCustomerJson).execute();

        model.addAttribute(ModelHelper.PERSON_REF, personRef);
        model.addAttribute(ModelHelper.CRMCUSTOMER, customer);
        model.addAttribute(ModelHelper.USERTOKEN, trim(userTokenXml));


        return "editdata";
    }


    @RequestMapping(value = "/crmdata", method = RequestMethod.GET)
    public String getCrmdata(HttpServletRequest request, Model model) {
        log.trace("getCrmdata was called");

        model.addAttribute("logoURL", LOGOURL);

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        log.warn("FOR TESTING ONLY");
        SSLTool.disableCertificateValidation();
        if (personRef == null || personRef.isEmpty()) {
            personRef = "1234";
        }

        String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef).execute();

        if (crmCustomerJson != null) {
            Customer customer = CustomerMapper.fromJson(crmCustomerJson);

            model.addAttribute(ModelHelper.PERSON_REF, personRef);
            model.addAttribute(ModelHelper.CRMCUSTOMER, customer);
            model.addAttribute(ModelHelper.USERTOKEN, trim(userTokenXml));
        }
        return "editdata";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public String setJsonCrmdata(HttpServletRequest request, Model model) throws IOException {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        ObjectMapper mapper = new ObjectMapper();
        Customer customer = mapper.readValue(request.getReader(), Customer.class);

        String customerJson = CustomerMapper.toJson(customer);

        String location = new CommandUpdateCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef, customerJson).execute();

        model.addAttribute(ModelHelper.JSON_DATA, customerJson);

        return "json";
    }


    @RequestMapping(value = "/profile", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getJsonCrmdata(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, tokenServiceClient.getMyAppTokenID(), userTokenId, personRef).execute();


        model.addAttribute(ModelHelper.JSON_DATA, crmCustomerJson);

        return "json";
    }



    @RequestMapping(value = "/verify/phone_send_pin", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String sendPhoneVerificationPin(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String phoneNo = request.getParameter("phoneNo");

        Boolean pinSent = new CommandSendPhoneVerificationPin(crmServiceUri, tokenServiceClient.getMyAppTokenXml(), userTokenId, personRef, phoneNo).execute();

        model.addAttribute(ModelHelper.JSON_DATA, ""+pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/phone_by_pin", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String verifyPhoneByPin(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String phoneNo = request.getParameter("phoneNo");
        String pin = request.getParameter("pin");

        Boolean pinSent = new CommandVerifyPhoneByPin(crmServiceUri, tokenServiceClient.getMyAppTokenXml(), userTokenId, personRef, phoneNo, pin).execute();

        model.addAttribute(ModelHelper.JSON_DATA, ""+pinSent);

        return "json";
    }


    @RequestMapping(value = "/verify/email_send_token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String sendEmailVerificationToken(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String email = request.getParameter("email");

        Boolean tokenSent = new CommandSendEmailVerificationToken(crmServiceUri, tokenServiceClient.getMyAppTokenXml(), userTokenId, personRef, email, emailVerificationLink).execute();

        model.addAttribute(ModelHelper.JSON_DATA, ""+tokenSent);

        return "json";
    }


    @RequestMapping(value = "/verify/email_by_token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String verifyEmailByToken(HttpServletRequest request, Model model) {

        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = tokenServiceClient.getUserTokenByUserTokenID(userTokenId);
        String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

        String email = request.getParameter("email");
        String token = request.getParameter("token");

        Boolean tokenVerified = new CommandVerifyEmailByToken(crmServiceUri, tokenServiceClient.getMyAppTokenXml(), userTokenId, personRef, email, token).execute();

        model.addAttribute(ModelHelper.JSON_DATA, ""+tokenVerified);

        return "json";
    }


    @RequestMapping(value = "/profilemock", method = RequestMethod.PUT)
    public String setMockJsonCrmdata(HttpServletRequest request, Model model) throws IOException {

        log.warn("Mock service: Parsing and returning data.");
        ObjectMapper mapper = new ObjectMapper();
        Customer customer = mapper.readValue(request.getReader(), Customer.class);

        model.addAttribute(ModelHelper.JSON_DATA, CustomerMapper.toJson(customer));

        return "json";
    }


    @RequestMapping(value = "/profilemock", method = RequestMethod.GET)
    public String getMockJsonCrmdata(HttpServletRequest request, Model model) {
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);

        log.warn("Mock service: Returning mock-data only");
        Customer customer = createMockCustomer(userTokenId);

        model.addAttribute(ModelHelper.JSON_DATA, CustomerMapper.toJson(customer));

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

        customer.getPhonenumbers().put("mobile", new PhoneNumber("44556677", null,  true));
        customer.getPhonenumbers().put("work-mobile", new PhoneNumber("98765432", null,  false));
        customer.getPhonenumbers().put("home-mobile", new PhoneNumber("43215678", null,  true));

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

}
