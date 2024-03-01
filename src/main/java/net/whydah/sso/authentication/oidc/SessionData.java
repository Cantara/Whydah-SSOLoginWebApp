package net.whydah.sso.authentication.oidc;

import lombok.Data;
import net.whydah.sso.authentication.iamproviders.BaseSessionData;

import java.io.Serializable;

@Data
public class SessionData extends BaseSessionData implements Serializable {
	private String refreshToken;
	private String accessToken;
	private Long expiryTimeSeconds;

	//OIDC
	private String subject;
	private String firstName;
	private String lastName;

	//OIDC "extra"
	private String email;
}
