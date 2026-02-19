package net.whydah.sso.extensions.crmcustomer;

import static net.whydah.sso.basehelpers.JsonPathHelper.getStringFromJsonpathExpression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.extensions.crmcustomer.types.DeliveryAddress;
import net.whydah.sso.extensions.crmcustomer.types.EmailAddress;
import net.whydah.sso.extensions.crmcustomer.types.PhoneNumber;

public class INNCRMCustomerMapper extends CustomerMapper {
	private final static Logger log = LoggerFactory.getLogger(INNCRMCustomerMapper.class);

	//Receive a customer json data, can be old or new format
	//Return - the Customer object should has a new format with "new delivery_address INN JSON format" addresses
	public static Customer fromJson(String customerJson) {
		Customer customer = CustomerMapper.fromJson(customerJson);
		customer = fromInnJson(customer, customerJson);
		fixCustomerLabels(customer);
		return customer;
	}

	//FOR TESTING ONLY
	public static String toInnDeliveryAddressJson(Customer c, String deliveryAddressId) {
		// Lets find the correct address;
		DeliveryAddress d = c.getDeliveryaddresses().get(deliveryAddressId);
		// Check if it is InnJson allready
		if (d != null && d.getAddressLine1().contains("deliveryaddress")) {
			return d.getAddressLine1();
		}
		// Create and set if not
		if (d != null) {

			String addressLine2 = "";
			if (d.getAddressLine2() != null) {
				addressLine2 = d.getAddressLine2();
			}

			String contactName = getDefaultName(c);
			PhoneNumber phone = getDefaultPhone(c);
			EmailAddress email = getDefaultEmail(c);

			String deliveryAddressJson = getDeliveryAddressJson(deliveryAddressId, contactName, "",
					d.getAddressLine1(), addressLine2, d.getPostalcode(), 
					d.getPostalcity(), "no", "", contactName, email!=null?email.getEmailaddress():"", phone!=null?phone.getPhonenumber():"",
							phone!=null?phone.isVerified():false, email!=null? email.isVerified():false,"", "", "");


			log.debug("toInnDeliveryAddressJson - constructed json:{}", deliveryAddressJson);
			return deliveryAddressJson;

		}
		return "{}";

	}

	//private use for fromInnJson(), to return a "new delivery_address INN JSON format"
	private static String convertToInnDeliveryAddress(Customer customer, String addressTag, String deliveryAddressJson){
		try {
			JSONObject addressJsonObj = new JSONObject(deliveryAddressJson);
			log.debug(addressJsonObj.toString());

			//add to the email list
			JSONObject contact= addressJsonObj.has("contact")? addressJsonObj.getJSONObject("contact"):null;
			if(contact!=null){
				String email = contact.has("email")? contact.getString("email"):"";
				if(email!=null){
					boolean found = false;
					for(EmailAddress ea : customer.getEmailaddresses().values()){
						if(ea.getEmailaddress().equalsIgnoreCase(email)){
							found = true;
						}
					}

					if(!found){
						String tag = UUID.randomUUID().toString();
						customer.getEmailaddresses().put(tag, new EmailAddress(email, tag, false));
					}
				}
			}

			if(addressJsonObj.has("name") && addressJsonObj.has("company") && addressJsonObj.has("addressLine1") && addressJsonObj.has("addressLine2")
					&& addressJsonObj.has("postalcode") && addressJsonObj.has("postalcity") && addressJsonObj.has("reference")
					&& addressJsonObj.has("tags") && addressJsonObj.has("contact") && addressJsonObj.has("deliveryinformation"))
			{	
				addressJsonObj.put("tags", addressTag);
				return "{\"deliveryaddress\": " + addressJsonObj.toString() + "}";            
			} else if(addressJsonObj.has("addressLine1"))
			{	
				try{
					//see if the addressLine1 contains a deliveryaddress
					JSONObject trialParser= new JSONObject(addressJsonObj.get("addressLine1").toString());
					if(trialParser.has("deliveryaddress")){
						JSONObject parser = trialParser.getJSONObject("deliveryaddress");
						parser.put("tags", addressTag);
						return trialParser.toString(); //no need to do anything, because it is a correct format like getDeliveryAddressJsonFormat()
					}
				} catch(Exception ex){

				}

				//if it is not a correct json format like the function getDeliveryAddressJsonFormat(), we should create one
				//NOTE: we should update this relevant data

				String defaultcontactName = getDefaultName(customer);
				PhoneNumber phone = getDefaultPhone(customer);
				EmailAddress email = getDefaultEmail(customer);
				String contactEmail = email.getEmailaddress();
				boolean isEmailVerified = false;

				String comment = "";
				String name = addressJsonObj.has("name")?addressJsonObj.getString("name").trim() :"";
				if(name==null||name.isEmpty()){
					name = defaultcontactName;
				}
				String addressLine1 = addressJsonObj.has("addressLine1")? addressJsonObj.getString("addressLine1") :"";
				String addressLine2 = addressJsonObj.has("addressLine2")? addressJsonObj.getString("addressLine2") :"";
				String postalcode = addressJsonObj.has("postalcode")? addressJsonObj.getString("postalcode") :"";
				String postalcity = addressJsonObj.has("postalcity")? addressJsonObj.getString("postalcity") :"";
				String company = addressJsonObj.has("company")? addressJsonObj.getString("company") :"";


				if(contact!=null){
					contactEmail = contact.has("email")? contact.getString("email"):"";


					if(contactEmail.equals("")){
						//use the default
						contactEmail = email!=null? email.getEmailaddress():"";
						isEmailVerified = email.isVerified();
					} else {
						if(customer.getEmailaddresses()!=null){
							for(EmailAddress ea : customer.getEmailaddresses().values()){
								if(ea.getEmailaddress().equals(contactEmail)){
									isEmailVerified = ea.isVerified();
								}
							}
						}
					}

				}

				JSONObject deliveryinformation = addressJsonObj.has("deliveryinformation")?addressJsonObj.getJSONObject("deliveryinformation"):null;
				if(deliveryinformation!=null){
					comment = deliveryinformation.has("additionalAddressInfo")? deliveryinformation.getString("additionalAddressInfo"):"";
				}


				return getDeliveryAddressJson(addressTag, name, company, addressLine1, addressLine2, postalcode, postalcity, "no", "" , name, contactEmail, phone!=null?phone.getPhonenumber():"",
						phone!=null?phone.isVerified():false, isEmailVerified, comment,"", "");
			}



		} catch (JSONException e1) {
		}
		return "";
	}

	//private use for fromJson(), the Customer object should has a new format with "new delivery_address INN JSON format" addresses
	private static Customer fromInnJson(Customer customer, String innJson) {

		if (customer == null) {
			log.warn("fromInnJson calles with customer=null");
			return null;
		}
		try {
			Map<String, DeliveryAddress> deliveryAddressMap = customer.getDeliveryaddresses();
			for (Map.Entry<String, DeliveryAddress> entry : deliveryAddressMap.entrySet()) {
				log.debug("fromInnJson: key:" + entry.getKey() + "/" + entry.getValue());
				String addressJson = (new net.minidev.json.JSONObject(JsonPathHelper.getJsonObjectFromJsonpathExpression(innJson, "$.deliveryaddresses['" + entry.getKey() + "']"))).toString();
				DeliveryAddress changeAddress = entry.getValue();
				changeAddress.setAddressLine1(convertToInnDeliveryAddress(customer, entry.getKey(), addressJson));
			}

			String defaultEmailLabel = getStringFromJsonpathExpression(innJson, "$.defaultEmailLabel");
			customer.setDefaultEmailLabel(defaultEmailLabel);
		} catch (Exception e) {
			log.error("Unable to parse the innJson data:{}\n Exception:{}", innJson, e);
		}
		log.debug("Json: {}", customer);
		return customer;
	}

	//for testing, update old customer which should contain "new delivery_address INN JSON format" addresses,
	//Return a whole Customer Json data
	public static String toInnJson(Customer customer) {
		String customerJson = INNCRMCustomerMapper.toJson(customer);
		String innCustomerJson = "";
		try {
			log.trace("Build json from Customer {}", customer);
			JSONObject innCustomer = new JSONObject(customerJson);
			log.debug("Built jsonArray: {}", innCustomer.toString());
			JSONObject deliveryAddresses = (JSONObject) innCustomer.get("deliveryaddresses");
			if (deliveryAddresses != null) {
				Map<String, DeliveryAddress> deliveryAddressMap = customer.getDeliveryaddresses();
				for (String addressLabel : deliveryAddressMap.keySet()) {
					DeliveryAddress da = deliveryAddressMap.get(addressLabel);
					if (!da.getAddressLine1().contains("deliveryaddress")) {
						// Old format, let us update

						String contactName = getDefaultName(customer);
						PhoneNumber phone = getDefaultPhone(customer);
						EmailAddress email = getDefaultEmail(customer);


						String deliveryAddressJson = getDeliveryAddressJson(addressLabel, contactName, "",
								da.getAddressLine1(), da.getAddressLine2(), da.getPostalcode(), 
								da.getPostalcity(), "no", "", contactName, email!=null?email.getEmailaddress():"", phone!=null?phone.getPhonenumber():"",
										phone!=null?phone.isVerified():false, email!=null? email.isVerified():false, "", "", "");

						log.debug("toInnJson - constructed json:{}", deliveryAddressJson);
						deliveryAddresses.put(addressLabel, deliveryAddressJson);

					}

				}
				innCustomer.put("deliveryaddresses", deliveryAddresses);
			}
			innCustomerJson = innCustomer.toString();
		} catch (JSONException e) {
			log.info("Failed to map to JSON array {}", customerJson, e);
			throw new RuntimeException(e);
		} catch (ClassCastException cce) {
			log.info("Failed ClassCastException - ignoring {}", customerJson, cce);
			return customerJson;

		}

		return innCustomerJson;

	}


	//private use, email for "new delivery_address INN JSON format"
	private static EmailAddress getDefaultEmail(Customer customer) {
		//email
		String defaultEmailLabel = customer.getDefaultEmailLabel();
		EmailAddress email=null;
		if(defaultEmailLabel!=null){
			email = customer.getEmailaddresses().get(defaultEmailLabel);
		}
		return email;
	}

	//private use, extract phone number for "new delivery_address INN JSON format"
	private static PhoneNumber getDefaultPhone(Customer customer) {
		PhoneNumber phone=null;
		String defaultPhoneLabel = customer.getDefaultPhoneLabel();
		if(defaultPhoneLabel!=null){
			phone = customer.getPhonenumbers().get(defaultPhoneLabel);
		}
		return phone;
	}

	//private use, extract proper name for "new delivery_address INN JSON format"
	private static String getDefaultName(Customer customer) {
		//name
		String contactName =customer.getFirstname() + " " + customer.getLastname();
		return contactName;
	}


	//private use, FOR TESTING ONLY
	//TODO: need to fully test, update all fields (some fields are currently hard-coded)
	public static String convertToInnDeliveryJsonString(JSONObject singleAddress) {
		try {
			String addressLine1Json = (String) singleAddress.get("addressLine1");
			if (addressLine1Json != null && addressLine1Json.contains("deliveryaddress")) {
				return addressLine1Json;
			}

			JSONObject addressLine1 = new JSONObject(addressLine1Json);
			if (addressLine1 != null) {
				JSONObject da = (JSONObject) addressLine1.get("deliveryaddress");
				if (da != null) {
					if (da.get("addressLine1").toString().contains("deliveryaddress")) {
						return da.toString();
					}


					String tags = " ";
					if (da.has("tags")) {
						tags = (String) da.get("tags");
					}
					String contact_name = "";
					JSONObject da_cn = (JSONObject) da.get("contact");
					JSONObject da_di = (JSONObject) da.get("deliveryinformation");

					singleAddress.put("tags", tags);
					String deliveryAddressJson = getDeliveryAddressJson(tags, da.getString("name"), da.getString("company"),
							addressLine1.toString(), da.getString("addressLine2"), da.getString("postalcode"), 
							da.getString("postalcity"), "no", "", da_cn.getString("name"), da_cn.getString("email"),da_cn.getString("phoneNumber"), 
							false, false, da_di.getString("additionalAddressInfo"), da_di.getString("pickupPoint"),da_di.getString("deliverytime"));

					log.debug("convertToInnDeliveryJsonString - constructed json:{}", deliveryAddressJson);

					return deliveryAddressJson;
				}
			}
		} catch (JSONException e) {
			log.trace("Failed to parse to JSONObject {}. Reason {}", singleAddress, e.getMessage());
		}

		return "";

	}

	//get data from the form parameters, and generate our "new delivery_address INN JSON format"
	//Return a new DeliveryAddress object with addressLine1 = "new delivery_address INN JSON format"
//	public static DeliveryAddress deliveryAddressFromFormParams(HttpServletRequest request, String addressTag) {
//
//		String email = SessionDao.instance.getFromRequest_Email(request);
//		String company = SessionDao.instance.getFromRequest_Company(request);
//		String name = SessionDao.instance.getFromRequest_Name(request);
//		String cellPhone = SessionDao.instance.getFromRequest_CellPhone(request);
//		String addressLine = SessionDao.instance.getFromRequest_AddressLine(request);
//		String addressLine1 = SessionDao.instance.getFromRequest_AddressLine1(request);
//
//		if (addressLine1.isEmpty()) {
//			addressLine1 = addressLine;
//		}
//		String addressLine2 = SessionDao.instance.getFromRequest_AddressLine2(request);
//		String postalCode = SessionDao.instance.getFromRequest_PostalCode2(request);
//		String city = SessionDao.instance.getFromRequest_PostalCity(request);
//		String countryCode = SessionDao.instance.getFromRequest_CountryCode(request);
//		if(countryCode==null ||countryCode.isEmpty()){
//			countryCode = "no";
//		}
//
//		String comment = SessionDao.instance.getFromRequest_Comment(request);
//
//		String deliveryAddressJson = getDeliveryAddressJson(addressTag, name,
//				company, addressLine1, addressLine2, postalCode, city, countryCode, "", name, email, cellPhone, false, false, comment,"","");
//
//		log.debug("deliveryAddressFromFormParams - constructed json:{}", deliveryAddressJson);
//
//		return fromInnDeliveryAddressJson(deliveryAddressJson);
//	}

	//Return a new DeliveryAddress object with addressLine1 = "new delivery_address INN JSON format"
	public static DeliveryAddress fromInnDeliveryAddressJson(String deliveryAddressJson) {
		DeliveryAddress deliveryAddress = new DeliveryAddress();
		deliveryAddress.setAddressLine1(deliveryAddressJson);
		return deliveryAddress;
	}

	//private use, get DeliveryAddress json data
	private static String getDeliveryAddressJson(String addressTag,
			String name, String company, String addressLine1, String addressLine2,
			String zipCode, String city, String countryCode,
			String reference, String contact_name, String contact_email, String contact_phone, 
			boolean isPhoneConfirmed, boolean isEmailConfirmed, 
			String deliveryinfo_additionalAddressInfo, String deliveryinfo_pickupPoint, String deliveryinfo_deliveryTime) {


		String deliveryAddressJson =  getDeliveryAddressJsonFormat().
				replaceAll("#name", name!=null?name:"").
				replaceAll("#company", company!=null?company:null).
				replaceAll("#addressLine1", addressLine1!=null?addressLine1:"").
				replaceAll("#addressLine2", addressLine2!=null?addressLine2:"").
				replaceAll("#postalcode", zipCode!=null?zipCode:"").
				replaceAll("#postalcity", city!=null?city:"").
				replaceAll("#countryCode", countryCode!=null?countryCode:"no").
				replaceAll("#reference", reference!=null?reference:"").
				replaceAll("#tags", addressTag!=null?addressTag:"").
				replaceAll("#contact_name", contact_name!=null?contact_name:name).
				replaceAll("#contact_email", contact_email!=null?contact_email:"").
				replaceAll("#emailConfirmed", String.valueOf(isEmailConfirmed)).
				replaceAll("#contact_phoneNumber", contact_phone!=null?contact_phone:"").
				replaceAll("#phoneNumberConfirmed", String.valueOf(isPhoneConfirmed)).
				replaceAll("#deliveryinformation_additionalAddressInfo", deliveryinfo_additionalAddressInfo!=null?deliveryinfo_additionalAddressInfo:"").
				replaceAll("#deliveryinformation_pickupPoint",  deliveryinfo_pickupPoint!=null?deliveryinfo_pickupPoint:"").
				replaceAll("#deliveryinformation_deliverytime", deliveryinfo_deliveryTime!=null?deliveryinfo_deliveryTime:"");

		return deliveryAddressJson;
	}

	//private use, and FOR TESTING
	public static String getDeliveryAddressJsonFormat() {

		String deliveryAddressJson = "{`deliveryaddress`: {" +
				"      `name`: `#name`," +
				"      `company`:`#company`," +
				"      `addressLine1`:`#addressLine1`," +
				"      `addressLine2`:`#addressLine2`," +
				"      `postalcode`:`#postalcode`," +
				"      `postalcity`:`#postalcity`," +
				"      `countryCode`:`#countryCode`," +
				"      `reference`:`#reference`," +
				"      `tags`:`#tags`," +
				"      `contact`: {" +
				"			`name`:`#contact_name`," +
				"			`email`:`#contact_email`," +
				"			`emailConfirmed`:`#emailConfirmed`," +
				"			`phoneNumber`:`#contact_phoneNumber`, " +
				"			`phoneNumberConfirmed`:`#phoneNumberConfirmed`" +
				"		}," +
				"      `deliveryinformation`: {" +
				"			`additionalAddressInfo`:`#deliveryinformation_additionalAddressInfo`," +
				"			`pickupPoint`:`#deliveryinformation_pickupPoint`," +
				"			`Deliverytime`:`#deliveryinformation_deliverytime`" +
				"		}" +
				"}" +
				"}";
		return deliveryAddressJson.replace('`', '\"').replaceAll("\t", "");
	}


	
	

	public static DeliveryAddress deliveryAddress(String addressTag, 
			String name, String company, String cellPhone, String email, String addressLine1, String addressLine2, 
			String zipcode, String city) {

		String deliveryAddressJson = INNCRMCustomerMapper.getDeliveryAddressJsonFormat();
		deliveryAddressJson = deliveryAddressJson.
				replaceAll("#name", String.valueOf(name)).
				replaceAll("#company", String.valueOf(company)).
				replaceAll("#addressLine1", String.valueOf(addressLine1)).
				replaceAll("#addressLine2", String.valueOf(addressLine2)).
				replaceAll("#postalCode", String.valueOf(zipcode)).
				replaceAll("#city", String.valueOf(city)).
				replaceAll("#countryCode", "no").
				replaceAll("#reference", "").
				replaceAll("#tags", String.valueOf(addressTag)).
				replaceAll("#contact_name", String.valueOf(name)).
				replaceAll("#contact_email", String.valueOf(email)).
				replaceAll("#emailConfirmed", "false").
				replaceAll("#contact_phoneNumber", String.valueOf(cellPhone)).
				replaceAll("#phoneNumberConfirmed", "false").
				replaceAll("#deliveryinformation_additionalAddressInfo", "").
				replaceAll("#deliveryinformation_pickupPoint", "").
				replaceAll("#deliveryinformation_deliverytime", "");
		DeliveryAddress newDeliveryAddress = new DeliveryAddress();
		newDeliveryAddress.setAddressLine1(deliveryAddressJson);
		return newDeliveryAddress;
	}
	
	public static Customer getTemplateCustomer(String phoneNo) {
		if (phoneNo == null) {
			phoneNo = "56565656";
		}
		Properties properties;
		try {
			properties = AppConfig.readProperties();

			String cellPhoneLabel = properties.getProperty("crmdata.label.cellphone");
			String emailLabel = properties.getProperty("crmdata.label.email");
			String addressLabel = properties.getProperty("crmdata.label.address");

			Customer customer = new Customer();
			// "{\"id\":null,\"firstname\":\"Not found\",\"middlename\":null,\"lastname\":\"\"}"
			customer.setFirstname("Not found");

			customer.setEmailaddresses(new HashMap<String, EmailAddress>());
			customer.setPhonenumbers(new HashMap<String, PhoneNumber>());
			customer.setDeliveryaddresses(new HashMap<String, DeliveryAddress>());
			customer.setMiddlename("Not found");
			customer.setLastname("Not found");

			//default email
			customer.setDefaultEmailLabel(emailLabel);
			String defaultEmail = "fill@me.in";
			EmailAddress email = new EmailAddress(defaultEmail, emailLabel, false);
			customer.getEmailaddresses().put(emailLabel, email);

			//default phonenumber
			customer.setDefaultPhoneLabel(cellPhoneLabel);
			PhoneNumber phoneNumber = new PhoneNumber(phoneNo, cellPhoneLabel, true);
			customer.getPhonenumbers().put(cellPhoneLabel, phoneNumber);


			//default address
			DeliveryAddress address = deliveryAddress(addressLabel, phoneNo);
			customer.getDeliveryaddresses().put(addressLabel, address);


			return customer;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}




	//the default labels (email/phone/address) must have their names configured correctly; otherwise font-end pages fail to display 
	public static Customer fixCustomerLabels(Customer customer){

		try {
			Properties properties = AppConfig.readProperties();
			String cellPhoneLabel = properties.getProperty("crmdata.label.cellphone");
			String emailLabel = properties.getProperty("crmdata.label.email");
			String addressLabel = properties.getProperty("crmdata.label.address");
			String phoneNo ="";

			if(!customer.getEmailaddresses().containsKey(emailLabel)){
				EmailAddress eA = customer.getEmailaddresses().get(customer.getDefaultEmailLabel());
				if(eA==null){
					eA = new EmailAddress("fill@me.in", emailLabel, false);
				}
				customer.getEmailaddresses().put(emailLabel, eA);
			}
			if(!customer.getPhonenumbers().containsKey(cellPhoneLabel)){
				PhoneNumber pN = customer.getPhonenumbers().get(customer.getDefaultPhoneLabel());
				if(pN==null){
					pN = new PhoneNumber("56565656", cellPhoneLabel, true);
					phoneNo = pN.getPhonenumber();
				}
				customer.getPhonenumbers().put(cellPhoneLabel, pN);
			}
			if(!customer.getDeliveryaddresses().containsKey(addressLabel)){
				DeliveryAddress defaultAddress = customer.getDeliveryaddresses().get(customer.getDefaultAddressLabel());
				if(defaultAddress==null){
					defaultAddress = deliveryAddress(addressLabel, phoneNo);
				}
				customer.getDeliveryaddresses().put(addressLabel, defaultAddress);
			}


		}catch(Exception ex){
			ex.printStackTrace();
		}
		return customer;
	}

	private static DeliveryAddress deliveryAddress(String addressTag, String phoneNo) {
		return deliveryAddress(addressTag, "Not Set", "", phoneNo, "fill@me.in", "Address1", null, "pno", "city");
	}


	
}
