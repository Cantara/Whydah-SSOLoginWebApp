package net.whydah.sso.authentication.oidc;

import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;
import net.whydah.sso.ddd.model.application.ApplicationTokenID;
import net.whydah.sso.ddd.model.user.UserTokenId;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


public class CommandSTSLogonUserBySharedSecrect extends BaseHttpPostHystrixCommand<String> {

    public static int DEFAULT_TIMEOUT = 6000;

    private String adminUserTokenId;
    private String phoneNo;
    private String secrect;
    private String userticket;

    public CommandSTSLogonUserBySharedSecrect(URI serviceUri, String applicationTokenId, String myAppTokenXml, String adminUserTokenId, String phoneNo, String secrect, String userticket) {
        this(serviceUri, applicationTokenId, myAppTokenXml, adminUserTokenId, phoneNo, secrect, userticket, DEFAULT_TIMEOUT);
    }

    public CommandSTSLogonUserBySharedSecrect(URI serviceUri, String applicationTokenId, String myAppTokenXml, String adminUserTokenId, String phoneNo, String secrect, String userticket, int timeout) {
        super(serviceUri, myAppTokenXml, applicationTokenId, "SSOAUserAuthGroup", timeout);
        this.phoneNo = phoneNo;
        this.secrect = secrect;
        this.userticket = userticket;
        this.adminUserTokenId = adminUserTokenId;

        if (serviceUri == null || !ApplicationTokenID.isValid(applicationTokenId) || !UserTokenId.isValid(adminUserTokenId) || phoneNo == null || secrect == null) {
            log.error("CommandSTSCreateAndLogonUserByPhoneNumberPin initialized with null-values - will fail tokenServiceUri:{} myAppTokenId:{}, myAppTokenXml:{}, ", adminUserTokenId, myAppTokenId, myAppTokenXml);
        }
        if (phoneNo.length() > 16 || phoneNo.length() < 7) {
            log.warn("Attempting to access with illegal phone number: {}", phoneNo);
        }
        
    }

    @Override
    protected String getTargetPath() {
        return "user/" + myAppTokenId + "/" + userticket + "/get_usertoken_by_shared_secrect";
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("applicationtokenid", myAppTokenId);
        data.put("userticket", userticket);
        data.put("secrect", secrect);
        data.put("apptoken", myAppTokenXml);
        data.put("adminUserTokenId", adminUserTokenId);
        data.put("phoneno", phoneNo);
       
        return data;
    }
}