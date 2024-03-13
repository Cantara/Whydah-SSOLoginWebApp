package net.whydah.sso.authentication.oidc;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSetRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.authentication.iamproviders.google.GoogleAuthResult;
import net.whydah.sso.ddd.model.application.RedirectURI;
import net.whydah.sso.utils.HttpConnectionHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

public class AuthHelper implements Provider {
	final static Logger log = LoggerFactory.getLogger(AuthHelper.class);

	private final String appUri;
	private final String provider;
	private final ClientID providerAppId;
	//private final String providerAppIdEscaped;
	private final Secret providerAppSecret;
	private final OIDCProviderMetadata providerMetadata;
	private final URI ssoCallBack;
	//private final String ssoCallBackEscaped;
	private final Scope scopes;
	private final OIDCClaimsRequest claims;

	private final SessionManagementHelper sessionManagementHelper;
	private final ClientAuthentication clientAuth;
	private final IDTokenValidator validator;

	public AuthHelper(String appUri, String provider, String issuerURL, String providerAppId, String providerAppSecret,
					  String[] scopes, SessionManagementHelper sessionManagementHelper) throws GeneralException, IOException, URISyntaxException {
		this.appUri = appUri;
		this.provider = provider;
		this.providerMetadata = OIDCProviderMetadata.resolve(new Issuer(issuerURL));
		this.providerAppId = new ClientID(providerAppId);
		//this.providerAppIdEscaped = URLEncoder.encode(providerAppId, StandardCharsets.UTF_8);
		this.providerAppSecret = new Secret(providerAppSecret);
		this.scopes = new Scope(this.providerMetadata.getScopes().toStringList().stream().filter(s1 -> Arrays.stream(scopes).anyMatch(s2 -> s2.equalsIgnoreCase(s1))).toArray(String[]::new));
		if (this.providerMetadata.getClaims().isEmpty()) {
			claims = null;
		} else {
			ClaimsSetRequest claimsSetRequest = new ClaimsSetRequest();
			List<String> wantedClaims = List.of("name", "email", "name", "phoneNumber", "openid");
			for (String claim: this.providerMetadata.getClaims()) {
				for (String wantedClaim: wantedClaims) {
					if (!wantedClaim.equalsIgnoreCase(claim)) {
						continue;
					}
					claimsSetRequest.add(wantedClaim);
				}
			}
			//Might want to move this to IdToken to potentially reduce a request
			claims = new OIDCClaimsRequest().withUserInfoClaimsRequest(claimsSetRequest);
		}
		//this.scopes = scopes;
		this.sessionManagementHelper = sessionManagementHelper;
		//this.ssoCallBack = ssoCallBack;
		this.ssoCallBack = new URI(appUri.replaceFirst("/$", "") + "/" + this.provider + "/auth");
		//this.ssoCallBackEscaped = URLEncoder.encode(this.ssoCallBack.toString(), StandardCharsets.UTF_8);
		// Not sure how to decide witch one to use.
		//this.clientAuth = new ClientSecretBasic(this.providerAppId, this.providerAppSecret);
		this.clientAuth = new ClientSecretPost(this.providerAppId, this.providerAppSecret);

		JWSAlgorithm alg = JWSAlgorithm.RS256;
		if (!this.providerMetadata.getIDTokenJWSAlgs().contains(alg)) {
			alg = this.providerMetadata.getAuthorizationJWSAlgs().get(0);
		}


		//Slightly unsure about what validator to use, there is also one for HMAC with the client secret. Could be a bool?
		if (this.providerMetadata.getJWKSet() != null) {
			this.validator = new IDTokenValidator(this.providerMetadata.getIssuer(), this.providerAppId, alg, this.providerMetadata.getJWKSet());
		} else if (false) {
			//This is a validator for HMAC
			this.validator = new IDTokenValidator(this.providerMetadata.getIssuer(), this.providerAppId, alg, this.providerAppSecret);
		} else {
			this.validator = new IDTokenValidator(this.providerMetadata.getIssuer(), this.providerAppId, alg, this.providerMetadata.getJWKSetURI().toURL());
		}
	}


	public String getAuthRedirect(String redirectURI) {
		// state parameter to validate response from Authorization server and nonce parameter to validate idToken
		String state = newState();//UUID.randomUUID().toString();
		String nonce = newNonce();//UUID.randomUUID().toString();
		RedirectURI clientRedirectURI = new RedirectURI(redirectURI);
		SessionManagementHelper.storeStateAndNonceInStates(state, nonce, clientRedirectURI.getInput());
		return getAuthorizationCodeUrl(state, nonce);
	}

	public String getAuthorizationCodeUrl(String state, String nonce) {
		AuthenticationRequest ar = new AuthenticationRequest.Builder(
				new ResponseType("code"),
				scopes,
				providerAppId,
				ssoCallBack)
				.claims(claims)
				.endpointURI(providerMetadata.getAuthorizationEndpointURI())
				.state(new State(state))
				.nonce(new Nonce(nonce))
				.build();
		return ar.toURI().toString();
		//String url = ar.toURI().toString();
		//SessionManagementHelper.storeStateAndNonceInStates(state, nonce, url);
		//return url;
		/*
		String sb = this.providerAuthorizationUrl +
				'?' +
				"client_id=" + this.providerAppIdEscaped +
				'&' +
				"redirect_uri=" + this.ssoCallBackEscaped +
				'&' +
				"scope=" + this.scope +
				'&' +
				"state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

		if (!StringUtils.isEmpty(nonce)) {
			sb += '&' +
				  "nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8);
		}
		return sb;
		 */
	}

	public String processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(String url) throws Throwable {
		AuthenticationResponse ar;
		try {
			ar = AuthenticationResponseParser.parse(new URI(url));
		} catch (com.nimbusds.oauth2.sdk.ParseException | URISyntaxException e) {
			throw new Exception(e);
		}
		StateData stateData = sessionManagementHelper.validateState(null, ar.getState().getValue());
		if (ar instanceof AuthenticationErrorResponse) {
			// The OpenID provider returned an error
			throw new Exception(ar.toErrorResponse().getErrorObject().toString());
		}

		// Retrieve the authorisation code
		AuthorizationCode code = ar.toSuccessResponse().getAuthorizationCode();
		AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, ssoCallBack);

		// Retrieve token
		TokenRequest request = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientAuth, codeGrant);
		log.debug(request.toHTTPRequest().getBody());
		var response = request.toHTTPRequest().send();
		log.debug(response.getBody());
		TokenResponse tr = OIDCTokenResponseParser.parse(response);

		if (!tr.indicatesSuccess()) {
			throw new Exception(tr.toErrorResponse().toJSONObject().toJSONString());
		}

		OIDCTokenResponse successResponse = (OIDCTokenResponse)tr.toSuccessResponse();

		// Get the ID and access token, the server may also return a refresh token
		JWT idToken = successResponse.getOIDCTokens().getIDToken();
		AccessToken accessToken = successResponse.getOIDCTokens().getAccessToken();
		RefreshToken refreshToken = successResponse.getOIDCTokens().getRefreshToken();

		// Don't think we want to use these claims, seems to be more of a hassle than using the id token directly
		ClaimsSet claims = validator.validate(idToken, new Nonce(stateData.getNonce()));

		commitDataState(null, idToken, accessToken, refreshToken);

		return stateData.getRedirectURI();
	}

	private static URI getFullURL(HttpServletRequest request) throws URISyntaxException {
		StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
		String queryString = request.getQueryString();

		if (queryString == null) {
			return new URI(requestURL.toString());
		} else {
			return new URI (requestURL.append('?').append(queryString).toString());
		}
	}

	public String processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(HttpServletRequest httpRequest) throws Throwable {
		AuthenticationResponse ar;
		try {
			ar = AuthenticationResponseParser.parse(getFullURL(httpRequest));
		} catch (com.nimbusds.oauth2.sdk.ParseException | URISyntaxException e) {
			throw new Exception(e);
		}
		StateData stateData = sessionManagementHelper.validateState(httpRequest.getSession(), ar.getState().getValue());
		if (ar instanceof AuthenticationErrorResponse) {
			// The OpenID provider returned an error
			throw new Exception(ar.toErrorResponse().getErrorObject().toString());
		}

		// Retrieve the authorisation code
		AuthorizationCode code = ar.toSuccessResponse().getAuthorizationCode();
		AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, ssoCallBack);

		// Retrieve token
		TokenRequest request = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientAuth, codeGrant);

		TokenResponse tr = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

		if (!tr.indicatesSuccess()) {
			throw new Exception(tr.toErrorResponse().toString());
		}

		OIDCTokenResponse successResponse = (OIDCTokenResponse)tr.toSuccessResponse();

		// Get the ID and access token, the server may also return a refresh token
		JWT idToken = successResponse.getOIDCTokens().getIDToken();
		AccessToken accessToken = successResponse.getOIDCTokens().getAccessToken();
		RefreshToken refreshToken = successResponse.getOIDCTokens().getRefreshToken();

		// Don't think we want to use these claims, seems to be more of a hassle than using the id token directly
		ClaimsSet claims = validator.validate(idToken, new Nonce(stateData.getNonce()));

		commitDataState(httpRequest, idToken, accessToken, refreshToken);

		return stateData.getRedirectURI();
	}

	public boolean containsAuthenticationCode(HttpServletRequest httpRequest) {
		Map<String, String[]> httpParameters = httpRequest.getParameterMap();

		//boolean isPostRequest = httpRequest.getMethod().equalsIgnoreCase("POST");
		boolean containsErrorData = httpParameters.containsKey("error");
		//boolean containIdToken = httpParameters.containsKey("id_token");
		boolean containsCode = httpParameters.containsKey("code");
		return containsErrorData || containsCode;
		//return isPostRequest && containsErrorData || containsCode || containIdToken;
	}

	private void commitDataState(HttpServletRequest httpRequest, JWT idToken, AccessToken accessToken, RefreshToken refreshToken) throws Exception {
		HTTPResponse httpResponse = new UserInfoRequest(providerMetadata.getUserInfoEndpointURI(), accessToken).toHTTPRequest().send();
		UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);

		if (!userInfoResponse.indicatesSuccess()) {
			ErrorObject error = userInfoResponse.toErrorResponse().getErrorObject();
			throw new Exception(String.format("Request for userinfo failed: %s - %s",
					error.getCode(),
					error.getDescription()));
		}

		UserInfoSuccessResponse successResponse = userInfoResponse.toSuccessResponse();
		UserInfo ui = successResponse.getUserInfo();
		JWTClaimsSet claimsJWT = idToken.getJWTClaimsSet();
		log.debug("Claims - {}: {}, {}, {}, {}, {}", provider, claimsJWT.getSubject(), ui.getEmailAddress(), ui.getPhoneNumber(), ui.getGivenName(), ui.getFamilyName());
		sessionManagementHelper.setAccessToken(httpRequest, accessToken.getValue());
		if (refreshToken != null) {
			sessionManagementHelper.setRefreshToken(httpRequest, refreshToken.getValue());
		}
		sessionManagementHelper.setExpiryTimeSeconds(httpRequest, claimsJWT.getExpirationTime().toInstant().getEpochSecond());
		sessionManagementHelper.setSubject(httpRequest, claimsJWT.getSubject());

		// Not sure if this is the best way to do it, could be better to get the user info and use that object. However, that seems like a extra neetwork call
		sessionManagementHelper.setEmail(httpRequest, ui.getEmailAddress());//claims.getStringClaim("email"));
		sessionManagementHelper.setPhoneNumber(httpRequest, ui.getPhoneNumber());//claims.getStringClaim("phone_number"));
		sessionManagementHelper.setFirstName(httpRequest, ui.getGivenName());//claims.getStringClaim("given_name"));
		sessionManagementHelper.setLastName(httpRequest, ui.getFamilyName());//claims.getStringClaim("family_name"));
	}

	public boolean isAuthenticated(HttpServletRequest request) {
		return sessionManagementHelper.hasSession(request) && sessionManagementHelper.getAccessToken(request) != null;
	}

	public boolean isAccessTokenExpired(HttpServletRequest httpRequest) {
		Long expiration = sessionManagementHelper.getExpiryTimeSeconds(httpRequest);
		boolean isValid = System.currentTimeMillis() <= (expiration + 10) * 1000;
		return !isValid;
	}

	public void updateAuthDataUsingSilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Throwable {
		String rt = sessionManagementHelper.getRefreshToken(httpRequest);
		if (rt == null || rt.isEmpty() || rt.isBlank()) {
			//Throw error?
			//Log error?
			return;
		}
		RefreshToken refreshToken = new RefreshToken(rt);
		AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(refreshToken);
		// Retrieve token
		TokenRequest request = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientAuth, refreshTokenGrant);

		TokenResponse tr = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

		if (!tr.indicatesSuccess()) {
			throw new Exception(tr.toErrorResponse().toString());
		}

		OIDCTokenResponse successResponse = (OIDCTokenResponse)tr.toSuccessResponse();

		// Get the ID and access token, the server may also return a refresh token
		//JWT idToken = successResponse.getOIDCTokens().getIDToken();
		AccessToken accessToken = successResponse.getOIDCTokens().getAccessToken();
		refreshToken = successResponse.getOIDCTokens().getRefreshToken();

		// Don't think we want to use these claims, seems to be more of a hassle than using the id token directly
		//ClaimsSet claims = validator.validate(idToken, new Nonce(stateData.getNonce()));

		//commitDataState(httpRequest, idToken, accessToken, refreshToken);
		sessionManagementHelper.setAccessToken(httpRequest, accessToken.getValue());
		if (refreshToken != null) {
			sessionManagementHelper.setRefreshToken(httpRequest, refreshToken.getValue());
		}
	}

	/*
	private AuthResult getAuthResultBySilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws Throwable {


		try {
			String refreshToken =  sessionManagementHelper.getRefreshToken(httpRequest);

			return new AuthResult(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.parseIdToken());

		} catch (Exception e) {
			throw new ServiceUnavailableException("authentication result was null");
		}
	}

	private void validateNonce(StateData stateData, String nonce) throws Exception {
		if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {
			throw new Exception(sessionManagementHelper.getFailedToValidateMessage() + " - could not validate nonce");
		}
	}

	private String getNonceClaimValueFromIdToken(String idToken) throws ParseException {
		return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("nonce");
	}

	private AuthResult getAuthResultByAuthCode(
			HttpServletRequest httpServletRequest,
			AuthorizationCode authorizationCode,
			String currentUri) throws ServiceUnavailableException {

		log.debug("currentUri='{}', authorizationCode='{}', ", currentUri, authorizationCode);

		GoogleAuthResult result;
		try {
			log.debug("provider: {}, providerAppId: {}", provider, providerAppId);

			GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
			              new NetHttpTransport(),
			              JacksonFactory.getDefaultInstance(),
			              "https://oauth2.googleapis.com/token",
			              providerAppId,
					      providerAppSecret,
			              authorizationCode.getValue(),
			              currentUri)  // Specify the same redirect URI that you use with your web
			                             // app. If you don't have a web version of your app, you can
			                             // specify an empty string.
			              .execute();

			String accessToken = tokenResponse.getAccessToken();
			String refreshToken = tokenResponse.getRefreshToken();
			GoogleIdToken idToken = tokenResponse.parseIdToken();
			result = new GoogleAuthResult(accessToken, refreshToken, idToken);

		} catch (Exception e) {
			StringWriter strWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(strWriter));
			log.error("While getting tokenResponse from {}: {}", provider, strWriter);
			throw new ServiceUnavailableException("authentication result was null");
		}
		return result;
	}

	/*
	private GoogleConnectionProperties getDomainAppConfig() throws MalformedURLException {
		//String domain = GoogleSessionManagementHelper.getSessionSelectedDomain(httpRequest);
		return new GoogleConnectionProperties(appUri, properties);
	}
	 */

	private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
		if (oidcResponse.getAuthorizationCode() == null) {
			throw new Exception(sessionManagementHelper.getFailedToValidateMessage() + " - unexpected set of artifacts received");
		}
	}

	private static boolean isAuthenticationSuccessful(AuthenticationResponse authResponse) {
		return authResponse instanceof AuthenticationSuccessResponse;
	}

	static String getResponseStringFromConn(HttpURLConnection conn) throws IOException {

		BufferedReader reader;
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} else {
			reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
		}
		StringBuilder stringBuilder= new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
		}

		return stringBuilder.toString();
	}

	public static String newNonce() {
		return new Nonce().getValue();
	}

	public static String newState() {
		return new State().getValue();
	}

	public void logout(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException {
		if (!isAuthenticated(httpRequest)) {
			return;
		}
		String token = sessionManagementHelper.getAccessToken(httpRequest);
		httpRequest.getSession().invalidate();
		sessionManagementHelper.removeSession(httpRequest, response);
		URI revoke = providerMetadata.getRevocationEndpointURI();
		if (revoke != null) {
			//Would like to replace this with nimbusds specific impl, however can't find it.
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("Content-type", "application/x-www-form-urlencoded");
			HttpConnectionHelper.get(revoke.toString() + "?token=" + token, null, params, null);
		}
	}

	public String provider() {
		return this.provider;
	}
}
