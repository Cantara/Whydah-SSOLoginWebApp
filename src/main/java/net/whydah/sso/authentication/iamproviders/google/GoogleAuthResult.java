package net.whydah.sso.authentication.iamproviders.google;

import java.io.Serializable;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthResult implements Serializable{

	private String accessToken;
	private String refreshToken;
	private GoogleIdToken idToken;
	
}
