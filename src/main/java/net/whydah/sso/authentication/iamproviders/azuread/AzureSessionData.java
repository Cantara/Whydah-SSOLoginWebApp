package net.whydah.sso.authentication.iamproviders.azuread;

import java.io.Serializable;
import java.util.Date;

import com.microsoft.aad.msal4j.IAccount;

import lombok.Data;
import net.whydah.sso.authentication.iamproviders.BaseSessionData;

@Data
public class AzureSessionData extends BaseSessionData implements Serializable {

	private String accessToken;
	private String idToken;
	private String aadTokenCache;
	private IAccount account;
	private Date expiresOnDate;
	
}
