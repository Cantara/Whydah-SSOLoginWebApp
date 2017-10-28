package net.whydah.sso.dao;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.commands.extensions.crmapi.*;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.SSLTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import java.net.URI;


public class CRMHelper {

	private final static Logger log = LoggerFactory.getLogger(CRMHelper.class);
	WhydahServiceClient serviceClient;
	URI crmServiceUri;
	String emailVerificationLink;

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

	public void getCrmdata_AddToModel(Model model, String userTokenXml) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();

                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef()).execute();
                log.info("addCrmdataToModel - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", crmCustomerJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);

				model.addAttribute(ConstantValue.PERSON_REF, userToken.getPersonRef());
				model.addAttribute(ConstantValue.CRMCUSTOMER, crmCustomerJson);
			} catch (Exception e) {
				log.warn("addCrmdataToModel CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
			}
		}
	}

	public String getCrmdata(String userTokenXml) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();

                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef()).execute();
                log.info("getCrmdata - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", crmCustomerJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
				return crmCustomerJson;
			} catch (Exception e) {
				log.warn("getCrmdata CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
			}
		}
		return null;

	}

	public void createCrmCustomer_AaddToModel(Model model, String userTokenXml, Customer customer) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		try {
			SSLTool.disableCertificateValidation();

			customer.setId(userToken.getPersonRef());
			String customerJson = CustomerMapper.toJson(customer);
            String returnedPersonRef = new CommandCreateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), customerJson).execute();

            log.info("Created CRMrecord for user - personRef={}, returnedPersonRef:{] - userTokenId:{} -  crmcustomer: {} - userTokenXml:{}", userToken.getPersonRef(), returnedPersonRef, userToken.getUserTokenId(), customerJson, userTokenXml);

			model.addAttribute(ConstantValue.PERSON_REF, returnedPersonRef);
			model.addAttribute(ConstantValue.CRMCUSTOMER, customerJson);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String updateCrmData(String userTokenXml, String updateJson) {
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);

		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();
                String location = new CommandUpdateCRMCustomer(crmServiceUri, serviceClient.getMyAppTokenID(), userToken.getUserTokenId(), userToken.getPersonRef(), updateJson).execute();
                log.info("updateCrmData - Got CRMrecord for user:{} - used personRef={}, appTokenID:{}, , userTokenXml:{}", updateJson, userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
				return location;
			} catch (Exception e) {
				log.warn("updateCrmData CRMrecord failed - used personRef={}, appTokenID:{}, userTokenXml:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml);
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
		
		UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
		
		if (userToken.getPersonRef().length() > 2) {
			try {
				SSLTool.disableCertificateValidation();
                Boolean tokenSent = new CommandSendEmailVerificationToken(crmServiceUri, serviceClient.getMyAppTokenXml(), userToken.getUserTokenId(), userToken.getPersonRef(), email, emailVerificationLink).execute();
                log.info("sendEmailVerificiationToken - used personRef={}, appTokenID:{}, userTokenXml:{},email:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
				return tokenSent;		
			} catch (Exception e) {
				log.warn("sendEmailVerificiationToken failed - used personRef={}, appTokenID:{}, userTokenXml:{},pin:{},phone:{}", userToken.getPersonRef(), serviceClient.getMyAppTokenID(), userTokenXml, email);
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

	

}
