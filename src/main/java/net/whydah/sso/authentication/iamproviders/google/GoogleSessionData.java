package net.whydah.sso.authentication.iamproviders.google;

import java.io.Serializable;

import lombok.Data;
import net.whydah.sso.authentication.iamproviders.BaseSessionData;

@Data
public class GoogleSessionData extends BaseSessionData implements Serializable {

	private String refreshToken;
	private String accessToken;
	private Long expiryTimeSeconds;
	//stuff more
	private String email;
	private String subject;
	private String firstName;
	private String lastName;
	
	
	
}
