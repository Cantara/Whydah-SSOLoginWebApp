package net.whydah.sso.authentication.iamproviders.azuread;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import lombok.Data;
import net.whydah.sso.authentication.iamproviders.BaseSessionData;
import net.whydah.sso.authentication.iamproviders.StateData;

@Data
public class AzureSessionData extends BaseSessionData implements Serializable {

	private String accessToken;
	private String aadTokenCache;
	private IAccount account;
	private Date expiresOnDate;
	
}
