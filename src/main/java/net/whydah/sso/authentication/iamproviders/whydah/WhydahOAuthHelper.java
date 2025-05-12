package net.whydah.sso.authentication.iamproviders.whydah;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;

import net.minidev.json.JSONObject;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.model.WhydahProvider;
import net.whydah.sso.utils.HttpConnectionHelper;

@Component
public class WhydahOAuthHelper {

	final static Logger log = LoggerFactory.getLogger(WhydahOAuthHelper.class);
	
	String MY_APP_URI;
	String SSO_CALL_BACK;
	Properties properties;
	Map<String, WhydahProvider> PROVIDERS;

	@PostConstruct
	public void init() throws IOException {
		properties = AppConfig.readProperties();
		MY_APP_URI = properties.getProperty("myuri");
		SSO_CALL_BACK = MY_APP_URI.replaceFirst("/$", "") + "/whydahauth";
		String integrationConfig = properties.getProperty("whydah.integration.providers");
		PROVIDERS = new WhydahOauthIntegrationConfig(properties).getProviderMap();
	}
	
	public Map<String, WhydahProvider> getProviders() {
		return PROVIDERS;
	}
	
	public boolean isAuthenticated(HttpServletRequest httpRequest, String provider) {
		return WhydahOAuthSessionManagementHelper.hasSession(httpRequest, provider) && 
				WhydahOAuthSessionManagementHelper.getAccessToken(httpRequest, provider) != null;
	}

	public String getAuthRedirect(HttpServletRequest httpRequest, String provider, String redirectURI) {
		String oauthUrl = PROVIDERS.get(provider).getOauthUrl().replaceFirst("/$", "");
		String oauthClientId =  PROVIDERS.get(provider).getOauthClientId();
		if(oauthUrl==null || oauthClientId ==null) {
			return MY_APP_URI;
		}
		State state = new State();
		Nonce nonce = new Nonce();
		ClientID clientID = new ClientID(oauthClientId);
		 
		WhydahOAuthSessionManagementHelper.storeStateAndNonceInSession(httpRequest.getSession(), provider, state.getValue(), nonce.getValue(), redirectURI);
			
		// Specify scope
		Scope scope = Scope.parse("openid email phone profile");
		
		
		// Compose the request
		AuthenticationRequest authenticationRequest = new AuthenticationRequest(
		  URI.create(oauthUrl + "/authorize"),
		  ResponseType.CODE,
		  scope, 
		  clientID, 
		  URI.create(SSO_CALL_BACK), state, nonce);

		  URI authReqURI = authenticationRequest.toURI();
		  return authReqURI.toString();
	}

	public boolean isAccessTokenExpired(HttpServletRequest httpRequest, String provider) {
		Long expiration = WhydahOAuthSessionManagementHelper.getExpiryTimeSeconds(httpRequest, provider);
		boolean isValid = System.currentTimeMillis() <= (expiration + 10) * 1000;
		return !isValid;
	}

	public void updateAuthDataUsingSilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String provider) throws Exception {
		
		String oauthUrl = PROVIDERS.get(provider).getOauthUrl().replaceFirst("/$", "");
		String oauthClientId =  PROVIDERS.get(provider).getOauthClientId();
		if(oauthUrl==null || oauthClientId ==null) {
			return ;
		}
		
		RefreshToken refreshToken = new RefreshToken( WhydahOAuthSessionManagementHelper.getRefreshToken(httpRequest, provider));
		AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(refreshToken);

		// The credentials to authenticate the client at the token endpoint
		ClientID clientID = new ClientID(oauthClientId);
		Secret clientSecret = new Secret("secret");
		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		// The token endpoint
		URI tokenEndpoint = new URI(oauthUrl + "/token");

		// Make the token request
		TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, refreshTokenGrant);

		TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

		if (! response.indicatesSuccess()) {
		    // We got an error response...
		    TokenErrorResponse errorResponse = response.toErrorResponse();
		}

		AccessTokenResponse successResponse = response.toSuccessResponse();

		// Get the access token, the refresh token may be updated
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		refreshToken = successResponse.getTokens().getRefreshToken();
		
		WhydahOAuthSessionManagementHelper.setAccessToken(httpRequest, provider, accessToken.getValue());
		WhydahOAuthSessionManagementHelper.setRefreshToken(httpRequest, provider, refreshToken.getValue());
		
		getUserInfoAndCommitData(httpRequest, accessToken, refreshToken, provider);
	}

	public StateData processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(HttpServletRequest httpRequest) throws Exception {
		
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
		StateData stateData = WhydahOAuthSessionManagementHelper.validateState(httpRequest.getSession(), params.get("state").get(0));
		
		AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
		if (isAuthenticationSuccessful(authResponse)) {
			AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
			// validate that OIDC Auth Response matches Code Flow (contains only requested artifacts)
			validateAuthRespMatchesAuthCodeFlow(oidcResponse, stateData.getDomain());
			
			getAuthResultByAuthCode(httpRequest, oidcResponse.getAuthorizationCode(), stateData.getDomain(), currentUri);
			
			return stateData;
			
		} else {
			AuthenticationErrorResponse oidcResponse = (AuthenticationErrorResponse) authResponse;
			throw new Exception("Request for auth code failed: %s - %s".formatted(
					oidcResponse.getErrorObject().getCode(),
					oidcResponse.getErrorObject().getDescription()));
		}
	}
	
	private void getAuthResultByAuthCode(HttpServletRequest httpRequest, AuthorizationCode code, String provider, String currentURI) throws Exception {
		String oauthUrl = PROVIDERS.get(provider).getOauthUrl().replaceFirst("/$", "");
		String oauthClientId =  PROVIDERS.get(provider).getOauthClientId();
		if(oauthUrl==null || oauthClientId ==null) {
			return ;
		}
		
		
		URI callback = new URI(currentURI);
		AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

		// The credentials to authenticate the client at the token endpoint
		ClientID clientID = new ClientID(oauthClientId);
		Secret clientSecret = new Secret("secret");
		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		// The token endpoint
		URI tokenEndpoint = new URI(oauthUrl + "/token");

		// Make the token request
		TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant);

		TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

		if (! response.indicatesSuccess()) {
		    // We got an error response...
		    TokenErrorResponse errorResponse = response.toErrorResponse();
		   
			throw new Exception("Request for auth code failed: %s - %s".formatted(
					errorResponse.getErrorObject().getCode(),
					errorResponse.getErrorObject().getDescription()));
		}

		AccessTokenResponse successResponse = response.toSuccessResponse();

		// Get the access token, the server may also return a refresh token
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
		
		getUserInfoAndCommitData(httpRequest, accessToken, refreshToken, provider);
	
		
	}
	
	private void getUserInfoAndCommitData(HttpServletRequest httpRequest, AccessToken accessToken, RefreshToken refreshToken, String provider) throws Exception {
		String oauthUrl = PROVIDERS.get(provider).getOauthUrl().replaceFirst("/$", "");
		String oauthClientId =  PROVIDERS.get(provider).getOauthClientId();
		if(oauthUrl==null || oauthClientId ==null) {
			return ;
		}
		UserInfoRequest userInfoReq = new UserInfoRequest(
				URI.create(oauthUrl + "/userinfo"),
				(BearerAccessToken) accessToken);

		HTTPResponse userInfoHTTPResp = null;
		try {
			userInfoHTTPResp = userInfoReq.toHTTPRequest().send();
		} catch (SerializeException | IOException e) {
			throw new Exception(e.getMessage());
		}

		UserInfoResponse userInfoResponse = null;
		try {
			userInfoResponse = UserInfoResponse.parse(userInfoHTTPResp);
		} catch (ParseException e) {
			throw new Exception(e.getMessage());
		}

		if (userInfoResponse instanceof UserInfoErrorResponse response) {
			ErrorObject error = response.getErrorObject();
			throw new Exception("Request for userinfo failed: %s - %s".formatted(
					error.getCode(),
					error.getDescription()));
		}

		UserInfoSuccessResponse successResponse = (UserInfoSuccessResponse) userInfoResponse;
		JSONObject claims = successResponse.getUserInfo().toJSONObject();
		String uid = claims.getAsString("uid");
		String sub = claims.getAsString("sub");
		String firstName = claims.getAsString("first_name");
		String lastName = claims.getAsString("last_name");
		String customerRef = claims.getAsString("customer_ref");
		String securityLevel = claims.getAsString("security_level");
		String email = claims.getAsString("email");
		String phone = claims.getAsString("phone");
		if(uid==null) {
			uid = customerRef;
		}
		
		//commit data to session store
		WhydahOAuthSessionManagementHelper.setAccessToken(httpRequest, provider, accessToken.getValue());
		WhydahOAuthSessionManagementHelper.setRefreshToken(httpRequest, provider, refreshToken.getValue());
		WhydahOAuthSessionManagementHelper.setEmail(httpRequest, provider, email);
		WhydahOAuthSessionManagementHelper.setCellPhone(httpRequest, provider, phone);
		WhydahOAuthSessionManagementHelper.setFirstName(httpRequest, provider, firstName);
		WhydahOAuthSessionManagementHelper.setLastName(httpRequest, provider, lastName);
		WhydahOAuthSessionManagementHelper.setPersonRef(httpRequest, provider, customerRef);
		WhydahOAuthSessionManagementHelper.setSubject(httpRequest, provider, sub);
		WhydahOAuthSessionManagementHelper.setUID(httpRequest, provider, uid);
		WhydahOAuthSessionManagementHelper.setSecurityLevel(httpRequest, provider, securityLevel);
		WhydahOAuthSessionManagementHelper.setExpiryTimeSeconds(httpRequest, provider, (System.currentTimeMillis() + (accessToken.getLifetime() * 1000))/1000);
		
	}

	private static boolean isAuthenticationSuccessful(AuthenticationResponse authResponse) {
		return authResponse instanceof AuthenticationSuccessResponse;
	}
	
	private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse, String provider) throws Exception {
		if (oidcResponse.getAuthorizationCode() == null) {
			throw new Exception("Failed to validate data received from " + provider + " Authorization service - unexpected set of artifacts received");
		}
	}

	public void logoutRedirect(HttpServletRequest httpRequest, HttpServletResponse response, String provider) throws UnsupportedEncodingException {
		if(isAuthenticated(httpRequest, provider)) {
			String token = WhydahOAuthSessionManagementHelper.getAccessToken(httpRequest, provider);
			httpRequest.getSession().invalidate();
			WhydahOAuthSessionManagementHelper.removeSession(httpRequest, response, provider);
			
			String oauthUrl = properties.getProperty(provider + ".oauth2_url").replaceFirst("/$", "");
			String oauthClientId = properties.getProperty(provider + ".oauth2_clientid");
			if(oauthUrl==null || oauthClientId ==null) {
				return;
			}
			
			
			String endSessionEndpoint = oauthUrl + "/logout?id_token_hint=" + token;
			String redirectUriFromClient = SessionDao.instance.getFromRequest_RedirectURI(httpRequest);
			String redirectUrl = MY_APP_URI + (redirectUriFromClient == null ? "/logout" : "/logout?redirectURI=" + redirectUriFromClient);
			String logoutUrl = endSessionEndpoint + "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUrl, "UTF-8");
			HttpConnectionHelper.get(logoutUrl, null, null, null);
		}
		
	}

}
