package net.whydah.sso.authentication.iamproviders.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import net.whydah.sso.authentication.iamproviders.ExternalIAMSSOSuppliers;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.ddd.model.application.RedirectURI;
import net.whydah.sso.utils.HttpConnectionHelper;

@Component
public class GoogleAuthHelper {


	final static Logger log = LoggerFactory.getLogger(GoogleAuthHelper.class);
	
	String MY_APP_URI;
	String SSO_CALL_BACK;
	Properties properties;

	@PostConstruct
	public void init() throws IOException {
		properties = AppConfig.readProperties();
		MY_APP_URI = properties.getProperty("myuri");
		SSO_CALL_BACK = MY_APP_URI.replaceFirst("/$", "") + "/googleauth";
	}

	private String extractDomain(String username) {
		String domain = null;
		if(username.contains("@")) {
			domain = username.split("@")[1];
		}
		return domain;
	}

	public String getAuthRedirect(HttpServletRequest httpRequest, String username, String redirectURI)
			throws IOException {
		// state parameter to validate response from Authorization server and nonce parameter to validate idToken
		String state = UUID.randomUUID().toString();
		String nonce = UUID.randomUUID().toString();
		RedirectURI clientRedirectURI = new RedirectURI(redirectURI);
		String domain = "gmail.com";
		if(username!=null && !username.isEmpty()) {
			domain = extractDomain(username);
		} 
		GoogleSessionManagementHelper.storeStateAndNonceInSession(httpRequest.getSession(), domain, state, nonce, clientRedirectURI.getInput());
		return getAuthorizationCodeUrl(domain, username, SSO_CALL_BACK, state, nonce);
	}

	public String getAuthorizationCodeUrl(String domain, String userhint, String callbackUrl, String state, String nonce) {
		
		GoogleConnectionProperties ggDomainProperties = new GoogleConnectionProperties(MY_APP_URI, properties);
		return
				 new GoogleAuthorizationCodeRequestUrl(ggDomainProperties.getClientId(),
						 ggDomainProperties.getSSOCallback(), Arrays.asList(
				 "https://www.googleapis.com/auth/userinfo.email",
				 "https://www.googleapis.com/auth/userinfo.profile"))
				 .setState(state)
				 .setAccessType("offline")
				 .set("login_hint", userhint)
				 .build();
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

	public String processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(HttpServletRequest httpRequest)
			throws Throwable {

		String currentUri = httpRequest.getRequestURL().toString();
		log.debug("receive callback", currentUri );
		if(!currentUri.startsWith("https")) {
			currentUri = currentUri.replaceFirst("http", "https");
		}
		String path = httpRequest.getServletPath();
		String queryStr = httpRequest.getQueryString();
		String fullUrl = currentUri + (queryStr != null ? "?" + queryStr : "");
		log.debug("reolved URI {}", fullUrl);

		Map<String, List<String>> params = new HashMap<>();
		for (String key : httpRequest.getParameterMap().keySet()) {
			params.put(key, Collections.singletonList(httpRequest.getParameterMap().get(key)[0]));
		}
		// validate that state in response equals to state in request
		StateData stateData = GoogleSessionManagementHelper.validateState(httpRequest.getSession(), params.get("state").get(0));

		AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
		if (isAuthenticationSuccessful(authResponse)) {
			AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
			// validate that OIDC Auth Response matches Code Flow (contains only requested artifacts)
			validateAuthRespMatchesAuthCodeFlow(oidcResponse);
			
			GoogleAuthResult result = getAuthResultByAuthCode(
					httpRequest,
					stateData.getDomain(),
					oidcResponse.getAuthorizationCode(),
					currentUri);

			//TODO: check this later
			// validate nonce to prevent reply attacks (code maybe substituted to one with broader access)
			//validateNonce(stateData, getNonceClaimValueFromIdToken(result.idToken()));
			
			//GoogleSessionManagementHelper.setSessionPrincipal(httpRequest, result);
			

			commitDataState(httpRequest, result);
			
			
			
			return stateData.getRedirectURI();
		} else {
			AuthenticationErrorResponse oidcResponse = (AuthenticationErrorResponse) authResponse;
			throw new Exception(String.format("Request for auth code failed: %s - %s",
					oidcResponse.getErrorObject().getCode(),
					oidcResponse.getErrorObject().getDescription()));
		}
	}

	private void commitDataState(HttpServletRequest httpRequest, GoogleAuthResult result) {
		
		GoogleSessionManagementHelper.setAccessToken(httpRequest, result.getAccessToken());
		GoogleSessionManagementHelper.setRefreshToken(httpRequest, result.getRefreshToken());
		GoogleSessionManagementHelper.setExpiryTimeSeconds(httpRequest, result.getIdToken().getPayload().getExpirationTimeSeconds());
		GoogleSessionManagementHelper.setEmail(httpRequest, result.getIdToken().getPayload().getEmail());
		GoogleSessionManagementHelper.setFirstName(httpRequest, (String) result.getIdToken().getPayload().get("given_name"));
		GoogleSessionManagementHelper.setLastName(httpRequest, (String) result.getIdToken().getPayload().get("family_name"));
		GoogleSessionManagementHelper.setSubject(httpRequest, result.getIdToken().getPayload().getSubject());
	}

	public boolean isAuthenticated(HttpServletRequest request) {
		return GoogleSessionManagementHelper.hasSession(request) && GoogleSessionManagementHelper.getAccessToken(request) != null;
	}

	public boolean isAccessTokenExpired(HttpServletRequest httpRequest) {
		Long expiration = GoogleSessionManagementHelper.getExpiryTimeSeconds(httpRequest);
		boolean isValid = System.currentTimeMillis() <= (expiration + 10) * 1000;
		return !isValid;
	}

	public void updateAuthDataUsingSilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Throwable {
		GoogleAuthResult result = getAuthResultBySilentFlow(httpRequest, httpResponse);
		commitDataState(httpRequest, result);
	}

	public void logoutRedirect(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException {

		if(isAuthenticated(httpRequest)) {
			String token = GoogleSessionManagementHelper.getAccessToken(httpRequest);
			httpRequest.getSession().invalidate();
			GoogleSessionManagementHelper.removeSession(httpRequest, response);
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("Content-type", "application/x-www-form-urlencoded");
			HttpConnectionHelper.get("https://accounts.google.com/o/oauth2/revoke?token=" + token, null, params, null);
		}
	}

	private GoogleAuthResult getAuthResultBySilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws Throwable {

		
		GoogleConnectionProperties app;
		try {
			String refreshToken =  GoogleSessionManagementHelper.getRefreshToken(httpRequest);
			
			app = getDomainAppConfig();
			GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
			              new NetHttpTransport(),
			              JacksonFactory.getDefaultInstance(),
			              refreshToken,
			              app.getClientId(),
			              app.getClientSecret()).execute();
			
			return new GoogleAuthResult(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.parseIdToken());
			
		} catch (Exception e) {
			throw new ServiceUnavailableException("authentication result was null");
		}
	}

	private void validateNonce(StateData stateData, String nonce) throws Exception {
		if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {
			throw new Exception(GoogleSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce");
		}
	}

	private String getNonceClaimValueFromIdToken(String idToken) throws ParseException {
		return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("nonce");
	}

	private GoogleAuthResult getAuthResultByAuthCode(
			HttpServletRequest httpServletRequest,
			String domain,
			AuthorizationCode authorizationCode,
			String currentUri) throws Throwable {

		log.debug("currentUri='{}', authorizationCode='{}', ", currentUri, authorizationCode);

		GoogleAuthResult result;
		GoogleConnectionProperties app;
		try {
			app = getDomainAppConfig();
			log.debug("app: {}", app);

			GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
			              new NetHttpTransport(),
			              JacksonFactory.getDefaultInstance(),
			              "https://oauth2.googleapis.com/token",
			              app.getClientId(),
			              app.getClientSecret(),
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
			log.error("While getting tokenResponse from Google: {}", strWriter);
			throw new ServiceUnavailableException("authentication result was null");
		}
		return result;
	}

	private GoogleConnectionProperties getDomainAppConfig() throws MalformedURLException {
		//String domain = GoogleSessionManagementHelper.getSessionSelectedDomain(httpRequest);
		return new GoogleConnectionProperties(MY_APP_URI, properties);
	}

	private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
		if (oidcResponse.getAuthorizationCode() == null) {
			throw new Exception(GoogleSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received");
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

}
