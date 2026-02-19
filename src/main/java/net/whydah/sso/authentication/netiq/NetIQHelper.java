package net.whydah.sso.authentication.netiq;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import net.whydah.sso.config.AppConfig;

public class NetIQHelper {
    private static final Logger log = LoggerFactory.getLogger(NetIQHelper.class);
    private Map<String, String> expectedHeaders = new HashMap<>();


    public NetIQHelper() {
        expectedHeaders.put("HTTP_DEPARTMENT", "SE");
        expectedHeaders.put("HTTP_FNAME", "Thor Henning");
        expectedHeaders.put("HTTP_LNAME", "Hetland");
        expectedHeaders.put("HTTP_EMAIL", "Thor-Henning.Hetland@altran.com");
        expectedHeaders.put("HTTP_USERNAME", "totto");
        expectedHeaders.put("HTTP_VIA", "cv-c.test.com (Access Gateway-ag-2EBD8AE7CD9A4BDF-30851)");
        expectedHeaders.put("HTTP_X_FORWARDED_FOR", "162.16.212.108");
        expectedHeaders.put("HTTP_X_FORWARDED_HOST", "cv-c.test.com:818");
        expectedHeaders.put("HTTP_X_FORWARDED_SERVER", "cvc.test.com");
        expectedHeaders.put("HTTP_CONNECTION", "Keep-Alive");

    }

    public static boolean verifyNetIQHeader(String headername,String value){
        try {
            Properties properties = AppConfig.readProperties();
            String expectedValue = properties.getProperty("logintype.netiq.header."+headername);
            if (expectedValue!=null && expectedValue.length()>1){
                if (value.indexOf(expectedValue,0)<0){
                    log.warn("NetIQ redirect verification failed.  Header: "+headername+" , expected: "+expectedValue+" , found "+value+" ");
                    return false;
                }
            }
        } catch (IOException ioe){
            log.warn("Not found NETIQ header for {}",headername);
        }
        return true;
    }
    public Enumeration getExpectedHeaders() {

        return Collections.enumeration(expectedHeaders.keySet());
    }

    public String getExpectedHeader(String headerName) {
        return  expectedHeaders.get(headerName);
    }


    public String getFirstName(HttpServletRequest request) {
        return request.getHeader("FNAME");//"Thor Henning";

    }

    public String getLastName(HttpServletRequest request) {
        return request.getHeader("LNAME"); // "Hetland";

    }

    public String getUserDetartment(HttpServletRequest request) {
        return request.getHeader("DEPARTMENT"); // "SE";

    }


    public String getUserName(HttpServletRequest request) {
        log.debug(request.getHeader("USERNAME"));
        return request.getHeader("USERNAME"); // "totto@totto.org";

    }

    public String getEmail(HttpServletRequest request) {
        return request.getHeader("EMAIL"); // "Thor-Henning.Hetland@altran.com";

    }


    public  Map.Entry<String, String> findNetIQUserFromRequest(HttpServletRequest request) {
        String accessToken = request.getHeader("VIA");
        String netIQUser = getUserName(request);
        Map.Entry<String, String> pair = new AbstractMap.SimpleImmutableEntry<>(accessToken, netIQUser);
        log.debug("Logged in NetIQ user: code=" + "" + ", AccessToken=" + accessToken + "\n netIQUserName: " + netIQUser);
        return pair;
    }

    public  String getNetIQUserAsXml(HttpServletRequest request) {

        StringBuilder strb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n ");
        strb.append("<user>\n");
        strb.append("    <params>\n");

        strb.append("        <netIQAccessToken>").append(request.getHeader("Via")).append( "</netIQAccessToken>\n");

        strb.append("        <userId>").append(this.getEmail(request)).append( "</userId>\n");
        strb.append("        <firstName>").append(convertEncoding(this.getFirstName(request))).append("</firstName>\n");
        strb.append("        <lastName>").append(convertEncoding(this.getLastName(request))).append("</lastName>\n");
        strb.append("        <username>").append(this.getEmail(request)).append("</username>\n");  // +UUID.randomUUID().toString()
        strb.append("        <email>").append(this.getEmail(request)).append( "</email>\n");

        strb.append("    </params> \n");
        strb.append("</user>\n");
        log.info(strb.toString());
        return strb.toString();
    }

    // A very primitive conversion of the result from NetIQ
    private String convertEncoding(String myString) {
        try {
            return new String(myString.getBytes("ISO-8859-1"), "UTF8");
        } catch (Exception e) {
            return myString;
        }
    }

}
