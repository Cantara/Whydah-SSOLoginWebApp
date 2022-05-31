package net.whydah.sso.authentication.iamproviders.whydah;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import lombok.Data;

@Data
public class WhydahOAuthResult {
	private String accessToken;
	private String refreshToken;
	private UserInfo userInfo;
}
