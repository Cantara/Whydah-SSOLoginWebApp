package net.whydah.sso.authentication.oidc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class SessionData extends BaseSessionData implements Serializable {
	private String refreshToken;
	private String accessToken;
	private Long expiryTimeSeconds;

	//OIDC
	private String subject;
	private String firstName;
	private String lastName;
	private String phoneNumber;

	//OIDC "extra"
	private String email;
	
	private String userInfoJsonString;
	private Map<String, Object> claimsSet = new HashMap<>();
}
