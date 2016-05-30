package net.whydah.sso.utils;

import net.whydah.sso.config.AppConfig;
import net.whydah.sso.extensions.crmcustomer.types.Customer;
import net.whydah.sso.extensions.crmcustomer.types.DeliveryAddress;
import net.whydah.sso.extensions.crmcustomer.types.EmailAddress;
import net.whydah.sso.extensions.crmcustomer.types.PhoneNumber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SignupHelper {

    public static Customer createCustomerFromParameters(String uid, String cellPhone, boolean phoneVerified, String firstName, String lastName, String email, String streetAddress, String zipcode, String city) {
        try {
            Properties properties = AppConfig.readProperties();
            String cellPhoneLabel = properties.getProperty("crmdata.label.cellphone");
            String emailLabel = properties.getProperty("crmdata.label.email");
            String addressLabel = properties.getProperty("crmdata.label.address");

            Customer customer = new Customer();
            customer.setId(uid);
            customer.setFirstname(firstName);
            customer.setLastname(lastName);

            PhoneNumber phone = new PhoneNumber(cellPhone, null, phoneVerified); //User is logged in using pin - cellphone is verified
            Map<String, PhoneNumber> phoneNumberMap = new HashMap<>();
            phoneNumberMap.put(cellPhoneLabel, phone);
            customer.setPhonenumbers(phoneNumberMap);
            customer.setDefaultPhoneLabel(cellPhoneLabel);

            EmailAddress emailAddress = new EmailAddress(email, null, false);
            Map<String, EmailAddress> emailAddressMap = new HashMap<>();
            emailAddressMap.put(emailLabel, emailAddress);
            customer.setEmailaddresses(emailAddressMap);
            customer.setDefaultEmailLabel(emailLabel);

            DeliveryAddress address = new DeliveryAddress();
            address.setAddressLine1(streetAddress);
            address.setPostalcode(zipcode);
            address.setPostalcity(city);

            Map<String, DeliveryAddress> deliveryAddressMap = new HashMap<>();
            deliveryAddressMap.put(addressLabel, address);
            customer.setDeliveryaddresses(deliveryAddressMap);
            customer.setDefaultAddressLabel(addressLabel);
            return customer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public static boolean isANumber(String s){
        return s.matches("[0-9]+") && s.length()>0;
        //.contains("[0-9]+");
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

    public static String getValueOrDefault(String value, String defaultValue) {
        return SignupHelper.isNotNullOrEmpty(value) ? value : defaultValue;
    }


}
