package net.whydah.sso.dao;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.whydah.sso.authentication.CookieManager;
import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.commands.extensions.crmapi.CommandCreateCRMCustomer;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.commands.extensions.crmapi.CommandSendEmailVerificationToken;
import net.whydah.sso.commands.extensions.crmapi.CommandSendPhoneVerificationPin;
import net.whydah.sso.commands.extensions.crmapi.CommandUpdateCRMCustomer;
import net.whydah.sso.commands.extensions.crmapi.CommandVerifyEmailByToken;
import net.whydah.sso.commands.extensions.crmapi.CommandVerifyPhoneByPin;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.errorhandling.AppException;
import net.whydah.sso.errorhandling.AppExceptionCode;
import net.whydah.sso.extensions.crmcustomer.INNCRMCustomerMapper;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.extensions.crmcustomer.types.DeliveryAddress;
import net.whydah.sso.extensions.crmcustomer.types.EmailAddress;
import net.whydah.sso.extensions.crmcustomer.types.PhoneNumber;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.SSLTool;


public class CRMHelper {

	private final static Logger log = LoggerFactory.getLogger(CRMHelper.class);
	WhydahServiceClient serviceClient;
	URI crmServiceUri;
	String emailVerificationLink;
	ObjectMapper mapper = new ObjectMapper();

	protected CRMHelper(WhydahServiceClient serviceClient, URI crmServiceUri, String emailVerificationLink){
		this.serviceClient = serviceClient;
		this.crmServiceUri = crmServiceUri;
		this.emailVerificationLink = emailVerificationLink;
	}

	public boolean userHasManyAddresses(String userTokenXml) {

		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();

                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef()).execute();
                log.info("userHasManyAddresses - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, userTokenXml:{}", crmCustomerJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);

				Customer customer = CustomerMapper.fromJson(crmCustomerJson);
				if (customer.getDeliveryaddresses().size()>1){
					return true;
				}

			} catch (Exception e) {
				log.warn("userHasManyAddresses CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
			}
		}
		log.warn("userHasManyAddresses CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
		return false;
	}

//HUY - refactor
//	public void getCrmdata_AddToModel(Model model, String userTokenXml) {
//		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
//
//		if (userToken.getPersonRef().length() > 2) {
//			try {
//				SSLTool.disableCertificateValidation();
//
//                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef()).execute();
//                log.info("addCrmdataToModel - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", crmCustomerJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//
//				model.addAttribute(ConstantValue.PERSON_REF, userToken.getPersonRef());
//				model.addAttribute(ConstantValue.CRMCUSTOMER, crmCustomerJson);
//			} catch (Exception e) {
//				log.warn("addCrmdataToModel CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//			}
//		}
//	}
	
	public String getCrmdata_AddToModel(Model model, String userTokenXml) throws AppException {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
		String crmCustomerJson = getCrmdata(userTokenXml);
		crmCustomerJson = crmCustomerJson.replace('`', '"').replace('\t', ' ');
		 try {
			 model.addAttribute(ConstantValue.CRMCUSTOMER, Pattern.compile("\\\\").matcher(mapper.writeValueAsString(crmCustomerJson)).replaceAll(""));
		 }catch(Exception ex) {
			 model.addAttribute(ConstantValue.CRMCUSTOMER, crmCustomerJson);
		 }
		model.addAttribute(ConstantValue.PERSON_REF, userToken.getPersonRef());
		return crmCustomerJson;
	}

	//HUY - refactor
//	public String getCrmdata(String userTokenXml) {
//		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
//
//		if (userToken.getPersonRef().length() > 2) {
//			try {
//				SSLTool.disableCertificateValidation();
//
//                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef()).execute();
//                log.info("getCrmdata - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", crmCustomerJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//				return crmCustomerJson;
//			} catch (Exception e) {
//				log.warn("getCrmdata CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//			}
//		}
//		return null;
//
//	}
	
	public String getCrmdata(String userTokenXml) throws AppException {
		if (userTokenXml != null) {
			UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
			if (userToken != null) {
				if (userToken.getPersonRef().length() > 0) {
					try {
						SSLTool.disableCertificateValidation();

						CommandGetCRMCustomer cmd= new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef());
						String crmCustomerJson = cmd.execute();
						Customer customer = null;
						boolean createNew = false;
						if(crmCustomerJson!=null) {
							customer = INNCRMCustomerMapper.fromJson(crmCustomerJson);
						} else if(cmd.getStatusCode()==404) {
							log.warn("user with personRef={} NOT FOUND for userTokenXml:{}. Try generating one customer record from UserToken info", userToken.getPersonRef(), userTokenXml);
							customer = generateCustomerForUsertoken(userToken);
							createNew = true;
						}
						if(customer!=null) {
							String innCrmCustomerJson = INNCRMCustomerMapper.toJson(customer);
							innCrmCustomerJson = innCrmCustomerJson.replace('`', '"').replace('\t', ' ');
							log.trace("getCrmdata - found CRMrecord for user:{} - used personRef={}, userTokenXml:{}", innCrmCustomerJson, userToken.getPersonRef(), userTokenXml);
							if(createNew) {
								new CommandCreateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), innCrmCustomerJson).execute();
							}
							return innCrmCustomerJson;
						} else {
							log.error("Trouble fetching crm data for user {}, with usertokenxml={} from the backend service. It maybe offline or too busy", userToken.getPersonRef(), userTokenXml);
							throw AppExceptionCode.USER_CRMSERVICE_FAILURE_4006.setDeveloperMessage("Failed to fetch crm data for user %s, with usertokenxml=%s from the backend service. It maybe offline or too busy".formatted(userToken.getPersonRef(), userTokenXml));
						}

					} catch (Exception e) {
						e.printStackTrace();
						log.error("Unexpected error when fetching CRM data for user {}, with usertokenxml={} from the backend service. Exception={}", userToken.getPersonRef(), userTokenXml, e.getMessage());
						throw e;
					}
				} else {
					throw AppExceptionCode.USER_INVALID_CRM_REFERENCE_4007;
				}
			} else {
				throw AppExceptionCode.USER_INVALID_USER_4004;
			}
		} else {
			throw AppExceptionCode.USER_USERTOKEN_NOT_FOUND_4000;
		}


	}
	
//HUY - migrate from INN
//	public void createCrmCustomer_AaddToModel(Model model, String userTokenXml, Customer customer) {
//		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
//
//		try {
//			SSLTool.disableCertificateValidation();
//
//			customer.setId(userToken.getPersonRef());
//			String customerJson = CustomerMapper.toJson(customer);
//            String returnedPersonRef = new CommandCreateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), customerJson).execute();
//
//            log.info("Created CRMrecord for user - personRef={}, returnedPersonRef:{] - userTokenId:{} -  crmcustomer: {} - userTokenXml:{}", userToken.getPersonRef(), returnedPersonRef, userToken.getUserTokenId(), customerJson, userTokenXml);
//
//			model.addAttribute(ConstantValue.PERSON_REF, returnedPersonRef);
//			model.addAttribute(ConstantValue.CRMCUSTOMER, customerJson);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}
	
	public String createCrmData_AaddToModel(Model model, String userTokenXml, Customer customer) throws AppException  {
		 
		 String customerJson = INNCRMCustomerMapper.toJson(customer).replace('`', '"').replace('\t', ' ');
		 String returnedPersonRef = createCrmData(userTokenXml, customerJson); 
		 model.addAttribute(ConstantValue.PERSON_REF, returnedPersonRef);
		 try {
			 model.addAttribute(ConstantValue.CRMCUSTOMER, Pattern.compile("\\\\").matcher(mapper.writeValueAsString(customerJson)).replaceAll(""));
		 }catch(Exception ex) {
			 model.addAttribute(ConstantValue.CRMCUSTOMER, customerJson);
		 }
        return returnedPersonRef;
	}
	
	public String createCrmData(String userTokenXml, String customerjs) throws AppException {
		
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
		String returnedPersonRef = null;
		if (userToken.getPersonRef().length() > 0) {
			
				SSLTool.disableCertificateValidation();
				CommandCreateCRMCustomer cmd = new CommandCreateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), customerjs);
				returnedPersonRef = cmd.execute();
				if(returnedPersonRef!=null) {
					log.info("Created CRMrecord for user - personRef={}, returnedPersonRef:{} - userTokenId:{} -  crmcustomer: {} - userTokenXml:{}", userToken.getPersonRef(), returnedPersonRef, userToken.getUserTokenId(), customerjs, userTokenXml);
					return returnedPersonRef;
				} else {
					
					throw AppExceptionCode.USER_CRMSERVICE_FAILURE_4006;
					
				}
			
		}
		return null;
	}
	
	public String createCrmData(String userTokenXml, Customer customer) throws AppException {
		
		String customerJson = INNCRMCustomerMapper.toJson(customer).replace('`', '"').replace('\t', ' ');
		return createCrmData(userTokenXml, customerJson); 
	}

//HUY - adjustment from OID
//	public String updateCrmData(String userTokenXml, String updateJson) {
//		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
//
//		if (userToken.getPersonRef().length() > 2) {
//			try {
//				SSLTool.disableCertificateValidation();
//                String location = new CommandUpdateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), updateJson).execute();
//                log.info("updateCrmData - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", updateJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//				return location;
//			} catch (Exception e) {
//				log.warn("updateCrmData CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
//			}
//		}
//		return null;
//
//	}
//	
	public String updateCrmData(String userTokenXml, String updateJson) throws AppException {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 0) {

			SSLTool.disableCertificateValidation();
			CommandUpdateCRMCustomer cmd;
			cmd = new CommandUpdateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), updateJson);
			String resultOfUpdate = cmd.execute();
			if(resultOfUpdate!=null) {
				log.info("updateCrmData - Got CRMrecord for updated user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}, \n   resultOfUpdate: {}"
						, updateJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, resultOfUpdate);

				return resultOfUpdate;
			} else {
				if(cmd.getStatusCode()!=404) { //fine with 404
					log.warn("updateCrmData CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
					throw AppExceptionCode.USER_CRMSERVICE_FAILURE_4006;
				}
			}

		}
		return null;

	}

	
	public boolean SendPhoneVerificationPin(String userTokenXml, String phoneNo) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();
                Boolean pinSent = new CommandSendPhoneVerificationPin(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), phoneNo).execute();
                log.info("SendPhoneVerificationPin - used personRef={}, appTokenID:{}, , userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
				return pinSent;		
			} catch (Exception e) {
				log.warn("SendPhoneVerificationPin failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
			}
		}
		return false;
	}


	public boolean SendPhoneVerificationPin(String userTokenXml, String phoneNo, String pin) {
		
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();
                Boolean pinSent = new CommandVerifyPhoneByPin(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), phoneNo, pin).execute();
                log.info("SendPhoneVerificationPin - used personRef={}, appTokenID:{}, userTokenXml:{},pin:{},phone:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, pin, phoneNo);
				return pinSent;		
			} catch (Exception e) {
				log.warn("SendPhoneVerificationPin failed - used personRef={}, appTokenID:{}, userTokenXml:{},pin:{},phone:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, pin, phoneNo);
			}
		}
		return false;
	}

	public boolean sendEmailVerificiationToken(String userTokenXml, String email) {
		
//		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
//		
//		if (userToken.getPersonRef().length() > 2) {
//			try {
//				SSLTool.disableCertificateValidation();
//                Boolean tokenSent = new CommandSendEmailVerificationToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), email, emailVerificationLink).execute();
//                log.info("sendEmailVerificiationToken - used personRef={}, appTokenID:{}, userTokenXml:{},email:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
//				return tokenSent;		
//			} catch (Exception e) {
//				log.warn("sendEmailVerificiationToken failed - used personRef={}, appTokenID:{}, userTokenXml:{},pin:{},phone:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
//			}
//		}
//		return false;
		
		if (email == null || email.length() < 5) {
			return false;
		}
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();

				String ticket = UUID.randomUUID().toString();

				if(SessionDao.instance.getServiceClient().createTicketForUserTokenID(ticket, userToken.getUserTokenId())) {
					CommandSendEmailVerificationToken cmd = new CommandSendEmailVerificationToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), email, emailVerificationLink, ticket, 30000);
					log.info("sendEmailVerificiationToken - used personRef={}, appTokenID:{}, email:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), email, userTokenXml);
					return cmd.execute();
				}
			} catch (Exception e) {
				log.warn("sendEmailVerificiationToken failed - used personRef={}, appTokenID:{}, email:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), email, userTokenXml);
			}
		}
		return false;

	}

	public boolean verifyEmailByToken(String userTokenXml, String email, String token) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
		
		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();
                Boolean tokenVerified = new CommandVerifyEmailByToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), email, token).execute();
                log.info("sendEmailVerificiationToken - used personRef={}, appTokenID:{}, userTokenXml:{},email:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
				return tokenVerified;		
			} catch (Exception e) {
				log.warn("sendEmailVerificiationToken failed - used personRef={}, appTokenID:{}, userTokenXml:{},pin:{},phone:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
			}
		}
		return false;
	}

	public static Customer generateCustomerForUsertoken(UserToken user) {

		Properties properties;
		try {
			properties = AppConfig.readProperties();

			String cellPhoneLabel = properties.getProperty("crmdata.label.cellphone");
			String emailLabel = properties.getProperty("crmdata.label.email");
			String addressLabel = properties.getProperty("crmdata.label.address");

			Customer customer = new Customer();
			customer.setId(user.getPersonRef());
			customer.setFirstname(user.getFirstName());
			customer.setMiddlename(null);
			customer.setLastname(user.getLastName());
			customer.setEmailaddresses(new HashMap<String, EmailAddress>());
			customer.setPhonenumbers(new HashMap<String, PhoneNumber>());
			customer.setDeliveryaddresses(new HashMap<String, DeliveryAddress>());
			//default email
			customer.setDefaultEmailLabel(emailLabel);
			String defaultEmail = user.getEmail();
			EmailAddress email = new EmailAddress(defaultEmail, emailLabel, false);
			customer.getEmailaddresses().put(emailLabel, email);

			//default phonenumber
			customer.setDefaultPhoneLabel(cellPhoneLabel);
			PhoneNumber phoneNumber = new PhoneNumber(user.getCellPhone(), cellPhoneLabel, true);
			customer.getPhonenumbers().put(cellPhoneLabel, phoneNumber);


			//default address
			DeliveryAddress address = INNCRMCustomerMapper.deliveryAddress(addressLabel, user.getFirstName() + user.getLastName(), "", user.getCellPhone(), user.getEmail(), "","","", "");
			customer.getDeliveryaddresses().put(addressLabel, address);

			return customer;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Customer createCustomerFromRequest(HttpServletRequest request) throws AppException {


		
		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
		String userTokenXml = SessionDao.instance.getServiceClient().getUserTokenByUserTokenID(userTokenId);
		String id = UserTokenXpathHelper.getPersonref(userTokenXml);
		if(id==null) {
			throw AppExceptionCode.USER_INVALID_CRM_REFERENCE_4007;
		}

		String crmCustomerJson = SessionDao.instance.getCRMHelper().getCrmdata(userTokenXml);
		Customer existingCustomer = null;
		if (crmCustomerJson != null) {
			existingCustomer = INNCRMCustomerMapper.fromJson(crmCustomerJson);
		}


		Customer customer = new Customer();
		customer.setId(id);
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
			boolean verified = isEmailVerified(existingCustomer, emailAddress);

			EmailAddress found = getEmailAddress(existingCustomer, label);
			if(found!=null){
				found.setEmailaddress(emailAddress);
				found.setVerified(verified);
				customer.getEmailaddresses().put(label, found);
			} else {
				EmailAddress email = new EmailAddress(emailAddress, null, verified);
				customer.getEmailaddresses().put(label, email);
			}

		}

		String[] phoneLabels = request.getParameterValues("phoneLabel");
		String defaultPhone = request.getParameter("defaultphone");
		customer.setDefaultPhoneLabel(defaultPhone);
		for (int i = 0; i < phoneLabels.length; i++) {
			String label = phoneLabels[i];
			String phonenumber = request.getParameter(label + "_phone");
			boolean verified = isPhoneNumberVerified(existingCustomer, phonenumber);

			PhoneNumber found = getPhoneNumber(customer, label);
			if(found!=null){
				found.setPhonenumber(phonenumber);
				found.setVerified(verified);
				customer.getPhonenumbers().put(label, found);
			} else {
				PhoneNumber phoneNumber = new PhoneNumber(phonenumber, null, verified);
				customer.getPhonenumbers().put(label, phoneNumber);
			}   
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

	public static PhoneNumber getPhoneNumber(Customer customer, String label){
		if(label ==null || label.equals("")) {
			return null;
		}
		return customer.getPhonenumbers().get(label);
	}

	public static boolean isPhoneNumberVerified(Customer customer, String phoneNumber){
		String label = queryPhoneNumberLabel(customer, phoneNumber);
		if(label==null){
			return false;
		} else {
			return customer.getPhonenumbers().get(label).isVerified();
		}
	}

	public static String queryPhoneNumberLabel(Customer customer, String phoneNumber){
		if(phoneNumber ==null || phoneNumber.equals("")) {
			return null;
		}
		Map<String, PhoneNumber> map = customer.getPhonenumbers();
		for(String key: map.keySet()){
			if(map.get(key).getPhonenumber().equalsIgnoreCase(phoneNumber)){
				return key;
			}
		}
		return null;
	}

	public static EmailAddress getEmailAddress(Customer customer, String label){
		if(label ==null || label.equals("")) {
			return null;
		}
		return customer.getEmailaddresses().get(label);
	}

	public static boolean isEmailVerified(Customer customer, String email){
		String label = queryEmailAddressLabel(customer, email);
		if(label==null){
			return false;
		} else {
			return customer.getEmailaddresses().get(label).isVerified();
		}
	}

	public static String queryEmailAddressLabel(Customer customer, String email){
		if(email ==null || email.equals("")) {
			return null;
		}
		Map<String, EmailAddress> map = customer.getEmailaddresses();
		for(String key: map.keySet()){
			if(map.get(key).getEmailaddress().equalsIgnoreCase(email)){
				return key;
			}
		}
		return null;
	}

	public static String updateEmailVerificationStatus(String customerJson, String[] verifiedEmails){
		List<String> _emails = new ArrayList<String>();
		for(String e : verifiedEmails){
			_emails.add(e.toLowerCase());
		}
		return updateEmailVerificationStatus(customerJson, _emails);
	}

	public static String fixEmailVerificationStatus(String customerJson){
		return updateEmailVerificationStatus(customerJson, new String[]{});
	}

	public static String updateEmailVerificationStatus(String customerJson, List<String> verifiedEmails){
		try {
			JSONObject obj = new JSONObject(customerJson);

			//update all email statuses in email_addresses
			Map<String, Boolean> emailStatusList = new HashMap<>();
			JSONObject emails = obj.getJSONObject("emailaddresses");
			Iterator itr = emails.keys();
			while(itr.hasNext()) {
				String field = itr.next().toString();
				JSONObject address = emails.getJSONObject(field);
				String email = address.getString("emailaddress");
				if(verifiedEmails.contains(email.toLowerCase())){
					address.put("verified", true);
				}
				boolean isVerified = address.getBoolean("verified"); 
				emailStatusList.put(email, isVerified);
			}


			//sync email statuses in deliveryaddresses (by checking emailaddresses)
			JSONObject deliveryAddresses = obj.getJSONObject("deliveryaddresses");
			itr = deliveryAddresses.keys();
			while(itr.hasNext()) {
				String field = itr.next().toString();
				JSONObject address = deliveryAddresses.getJSONObject(field);
				if(address!=null){

					if(address.has("contact")){
						JSONObject contact = address.getJSONObject("contact");
						if(contact.has("email")){
							String email = contact.getString("email");
							if(emailStatusList.containsKey(email.toLowerCase())){
								contact.put("emailConfirmed", emailStatusList.get(email));
							} else {
								contact.put("emailConfirmed", false);
								//add this new email to the list
								emails.put(UUID.randomUUID().toString(), new JSONObject(new EmailAddress(email, null, false)));
							}

						}

					}

					if(address.has("addressLine1")){
						if(!address.getString("addressLine1").equals("") && !address.getString("addressLine1").equals("null")){
							JSONObject addressLine1 = new JSONObject(address.getString("addressLine1"));
							if(addressLine1.has("deliveryaddress")){
								JSONObject deliveryAddress = addressLine1.getJSONObject("deliveryaddress");
								JSONObject contact = deliveryAddress.getJSONObject("contact");
								if(contact.has("email")){
									String email = contact.getString("email");
									if(emailStatusList.containsKey(email.toLowerCase())){
										contact.put("emailConfirmed", emailStatusList.get(email));
									} else {
										contact.put("emailConfirmed", false);
										//add this new email to the list
										emails.put(UUID.randomUUID().toString(), new JSONObject(new EmailAddress(email, null, false)));
									}
									address.put("addressLine1", addressLine1.toString());
								}
							}
						}


					}

					if(address.has("addressLine2")){
						if(!address.getString("addressLine2").equals("") && !address.getString("addressLine2").equals("null")){
							JSONObject addressLine2 = new JSONObject(address.getString("addressLine2"));
							if(addressLine2.has("deliveryaddress")){
								JSONObject deliveryAddress = addressLine2.getJSONObject("deliveryaddress");
								JSONObject contact = deliveryAddress.getJSONObject("contact");
								if(contact.has("email")){
									String email = contact.getString("email");
									if(emailStatusList.containsKey(email.toLowerCase())){
										contact.put("emailConfirmed", emailStatusList.get(email));
									} else {
										contact.put("emailConfirmed", false);
										//add this new email to the list
										emails.put(UUID.randomUUID().toString(), new JSONObject(new EmailAddress(email, null, false)));
									}
									address.put("addressLine2", addressLine2.toString());

								}
							}
						}
					}

				}

			}


			return obj.toString();

		} catch (JSONException e) {
			return customerJson;
		}
	}

	//get address_label
	public static String getDeliveryAddressLabel(String delivery_address_json){
		try{
			JSONObject innAddress = new JSONObject(delivery_address_json);
			JSONObject da = (JSONObject) innAddress.get("deliveryaddress");
			if(da!=null){
				if (da.has("tags")) {
					return (String) da.get("tags");
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}

}
