package net.whydah.sso.extensions.crmcustomer;

import java.net.URI;

import net.whydah.sso.commands.extensions.crmapi.CommandCreateCRMCustomer;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.config.ModelHelper;
import net.whydah.sso.config.SessionHelper;
import net.whydah.sso.extensions.crmcustomer.mappers.CustomerMapper;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.util.SSLTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;


public class CRMHelper {

    private final static Logger log = LoggerFactory.getLogger(SessionHelper.class);


    static void addCrmdataToModel(URI crmServiceUri, String appTokenID, Model model, String userTokenXml) {
        String personRef = net.whydah.sso.user.helpers.UserTokenXpathHelper.getPersonref(userTokenXml);

        if (personRef.length() > 2) {
            try {
                SSLTool.disableCertificateValidation();
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);

                String crmCustomerJson = new CommandGetCRMCustomer(crmServiceUri, appTokenID, userTokenId, personRef).execute();
                log.trace("Got CRMrecord for user - used personRef={}", personRef);

                model.addAttribute(ModelHelper.PERSON_REF, personRef);
                model.addAttribute(ModelHelper.CRMCUSTOMER, crmCustomerJson);
                model.addAttribute(ModelHelper.JSON_DATA, crmCustomerJson);

            } catch (Exception e) {

            }
        }
    }

    static void createCrmCustomer(URI crmServiceUri, String appTokenID, Model model, String userTokenXml, Customer customer) {
        try {
            String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
            String personRef = UserTokenXpathHelper.getPersonref(userTokenXml);

            SSLTool.disableCertificateValidation();

            String customerJson = CustomerMapper.toJson(customer);
            personRef = new CommandCreateCRMCustomer(crmServiceUri, appTokenID, userTokenId, personRef, customerJson).execute();

            log.trace("Created CRMrecord for user - personRef={}", personRef);

            model.addAttribute(ModelHelper.PERSON_REF, personRef);
            model.addAttribute(ModelHelper.CRMCUSTOMER, customerJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
