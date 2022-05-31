package net.whydah.sso.authentication.iamproviders.whydah;

import java.io.Serializable;

import lombok.Data;
import net.whydah.sso.authentication.iamproviders.BaseSessionData;

@Data
public class WhydahOauthSessionData extends BaseSessionData implements Serializable {

	public WhydahOauthSessionData() {
		super.setDomain("rebel"); //as default
	}
	
	private String refreshToken;
	private String accessToken;
	private Long expiryTimeSeconds;
	//stuff more
	private String uid;
	private String email;
	private String cellPhone;
	private String subject;
	private String firstName;
	private String lastName;
	private String personRef;
	private String securityLevel;
	
}
