package net.whydah.sso.authentication.iamproviders.azuread;


import com.microsoft.aad.msal4j.*;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

import net.whydah.sso.authentication.iamproviders.ExternalIAMSSOSuppliers;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.ddd.model.application.RedirectURI;
import net.whydah.sso.slack.SlackNotificationService;
import net.whydah.sso.utils.ToStringHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
public class AzureADAuthHelper {
	
	final static Logger log = LoggerFactory.getLogger(AzureADAuthHelper.class);
	
	

	private String MY_APP_URI;
	private String SSO_CALL_BACK;
	private String AAD_AUTHORITY_URL;
	private String AAD_GRAPH_URL;
	
	@PostConstruct
	public void init() throws IOException {
		Properties properties = AppConfig.readProperties();
		MY_APP_URI = properties.getProperty("myuri");
		SSO_CALL_BACK = MY_APP_URI.replaceFirst("/$", "") + "/aadauth";
		AAD_AUTHORITY_URL = properties.getProperty("aad.authority"); //fixed in app config
		AAD_GRAPH_URL = properties.getProperty("aad.msGraphEndpointHost"); //fixed in app config
	}

	/* not used
	public void sendAuthRedirect(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws IOException {
		httpResponse.setStatus(302);
		String authorizationCodeUrl = getAuthRedirect(httpRequest, httpRequest.getParameter(ConstantValue.LOGIN_HINT), httpRequest.getParameter("redirectURI"));
		httpResponse.sendRedirect(authorizationCodeUrl);
	}
	*/
	
	private String extractDomain(String username) {
		String domain = null;
		if(username !=null && username.contains("@")) {
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
		String domain = extractDomain(username);
		
		//we store redirectURL in the state since we can't attach it to the microsoft authorization request (forbidden due to security issues)
		AzureSessionManagementHelper.storeStateAndNonceInSession(httpRequest.getSession(), domain, state, nonce, clientRedirectURI.getInput());
		return getAuthorizationCodeUrl(domain, username, httpRequest.getParameter("claims"), null, SSO_CALL_BACK, state, nonce);
	}

	public String getAuthorizationCodeUrl(String domain, String loginhint, String claims, String scope, String callbackUrl, String state, String nonce)
			throws IOException {

		String updatedScopes = scope == null ? "" : scope;

		AzureADConnectionProperties azureADConnectionProperties = new AzureADConnectionProperties(ExternalIAMSSOSuppliers.configurationForDomain(domain));
		PublicClientApplication pca = PublicClientApplication.builder(azureADConnectionProperties.getClientId()).authority(getAuthorityURL(domain)).build();

		AuthorizationRequestUrlParameters parameters =
				AuthorizationRequestUrlParameters
				.builder(callbackUrl,
						Collections.singleton(updatedScopes))
				.responseMode(ResponseMode.QUERY)
				.prompt(Prompt.SELECT_ACCOUNT)
				.state(state)
				.nonce(nonce)
				.loginHint(loginhint)
				.claimsChallenge(claims)
				.build();

		return pca.getAuthorizationRequestUrl(parameters).toString();
	}

	public boolean containsAuthenticationCode(HttpServletRequest httpRequest) {
		Map<String, String[]> httpParameters = httpRequest.getParameterMap();

		boolean isPostRequest = httpRequest.getMethod().equalsIgnoreCase("POST");
		boolean containsErrorData = httpParameters.containsKey("error");
		boolean containIdToken = httpParameters.containsKey("id_token");
		boolean containsCode = httpParameters.containsKey("code");

		return isPostRequest && containsErrorData || containsCode || containIdToken;
	}

	public StateData processAuthenticationCodeRedirectAndReturnTheClientRedirectUrl(HttpServletRequest httpRequest)
			throws Throwable {

//		String currentUri = httpRequest.getRequestURL().toString();
		log.debug("receive callback from AAD {}", httpRequest.getRequestURL().toString());
//		if (!currentUri.startsWith("https")) {
//			currentUri = currentUri.replaceFirst("http", "https");
//		}
		//String path = httpRequest.getServletPath();
		String queryStr = httpRequest.getQueryString();
		String fullUrl = SessionDao.instance.MY_APP_URI.replaceFirst("/$", "") + "/aadauth"+ (queryStr != null ? "?" + queryStr : "");
		log.debug("reolved URI {}", fullUrl);

		Map<String, List<String>> params = new HashMap<>();
		log.debug("resolving params - keys {}", httpRequest.getParameterMap().keySet());
		log.debug("resolving params ", httpRequest.getParameterMap());
		for (String key : httpRequest.getParameterMap().keySet()) {
			params.put(key, Collections.singletonList(httpRequest.getParameterMap().get(key)[0]));
		}
		// validate that state in response equals to state in request
		log.debug("Validating state: " + params.get("state"));
		log.debug("Validating state.get(0): " + params.get("state").get(0));
		StateData stateData = AzureSessionManagementHelper.validateState(httpRequest.getSession(), params.get("state").get(0));

		AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
		if (AzureADAuthHelper.isAuthenticationSuccessful(authResponse)) {
			AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
			// validate that OIDC Auth Response matches Code Flow (contains only requested artifacts)
			validateAuthRespMatchesAuthCodeFlow(oidcResponse);

			IAuthenticationResult result = getAuthResultByAuthCode(
					httpRequest,
					stateData.getDomain(),
					oidcResponse.getAuthorizationCode(),
					SessionDao.instance.MY_APP_URI.replaceFirst("/$", "") + "/aadauth");

			// validate nonce to prevent reply attacks (code maybe substituted to one with broader access)
			validateNonce(stateData, getNonceClaimValueFromIdToken(result.idToken()));
			
			AzureSessionManagementHelper.setSessionSelectedDomain(httpRequest, stateData.getDomain());
			
			commitDataState(httpRequest, result);
			
			

			return stateData;
		} else {
			AuthenticationErrorResponse oidcResponse = (AuthenticationErrorResponse) authResponse;
			throw new Exception(String.format("Request for auth code failed: %s - %s",
					oidcResponse.getErrorObject().getCode(),
					oidcResponse.getErrorObject().getDescription()));
		}
	}

	private void commitDataState(HttpServletRequest httpRequest, IAuthenticationResult result) {
		AzureSessionManagementHelper.setAccount(httpRequest, result.account());
		AzureSessionManagementHelper.setAccessToken(httpRequest, result.accessToken());
		AzureSessionManagementHelper.setExpiresOnDate(httpRequest, result.expiresOnDate());
	}

	public boolean isAuthenticated(HttpServletRequest request) {
		return AzureSessionManagementHelper.hasSession(request) && AzureSessionManagementHelper.getAccessToken(request)!=null;
	}

	public boolean isAccessTokenExpired(HttpServletRequest httpRequest) {
		return AzureSessionManagementHelper.getExpiresOnDate(httpRequest).before(new Date());
	}

	public void updateAuthDataUsingSilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Throwable {
		IAuthenticationResult authResult = getAuthResultBySilentFlow(httpRequest, httpResponse);
		commitDataState(httpRequest, authResult);
	}

	public String getUserInfoFromGraph(String accessToken) throws Exception {
		// Microsoft Graph user endpoint
		URL url = new URL(AAD_GRAPH_URL.replaceFirst("/$", "") + "/beta/me");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		// Set the appropriate header fields in the request header.
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		conn.setRequestProperty("Accept", "application/json");

		String response = getResponseStringFromConn(conn);

		int responseCode = conn.getResponseCode();
//		if(responseCode == HttpURLConnection.HTTP_OK) {
//			 return response;
//		} else if(responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
//			return null;
//		} else {
//			throw new IOException(response);
//		}
		if(responseCode == HttpURLConnection.HTTP_OK) {
			 return response;
		} else {
			new IOException(response).printStackTrace();
			return null;
		}
	}

	public String logoutRedirect(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException {
		
		if(isAuthenticated(httpRequest)) {
			httpRequest.getSession().invalidate();
			AzureSessionManagementHelper.removeSession(httpRequest, response);
			String endSessionEndpoint = getAuthorityURL(AzureSessionManagementHelper.getSessionSelectedDomain(httpRequest)) + "/oauth2/logout";
			String redirectUriFromClient = SessionDao.instance.getFromRequest_RedirectURI(httpRequest);
			String realEstate = httpRequest.getParameter("real_estate");
			if(redirectUriFromClient != null && redirectUriFromClient.equalsIgnoreCase("welcome")) {
				redirectUriFromClient += "&real_estate=" + realEstate;
			}
			String redirectUrl = MY_APP_URI + (redirectUriFromClient == null ? "/logout" : "/logout?redirectURI=" + redirectUriFromClient);
			return endSessionEndpoint + "?post_logout_redirect_uri=" +
			URLEncoder.encode(redirectUrl, "UTF-8");
		} else {
			return null;
		}
	}

	public String getAppRoleAssignments(String accessToken) throws IOException {
		//GET    https://graph.microsoft.com/beta/users/{id | userPrincipalName}/appRoleAssignments
		URL url = new URL(AAD_GRAPH_URL.replaceFirst("/$", "") + "/beta/me/appRoleAssignments");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		// Set the appropriate header fields in the request header.
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		conn.setRequestProperty("Accept", "application/json");

		String response = getResponseStringFromConn(conn);
		int responseCode = conn.getResponseCode();
		if (responseCode != HttpURLConnection.HTTP_OK) {
			new IOException(response).printStackTrace();
			return null;
		}

		return response;


	}


	private IAuthenticationResult getAuthResultBySilentFlow(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws Throwable {

		//IAuthenticationResult result =  AzureSessionManagementHelper.getAuthSessionObject(httpRequest);

		String tokencache = AzureSessionManagementHelper.getTokenCache(httpRequest);
		IAccount account = AzureSessionManagementHelper.getAccount(httpRequest);
		IConfidentialClientApplication app = createClientApplicationSilentFlow(httpRequest);

		if (tokencache != null) {
			app.tokenCache().deserialize(tokencache);
		}

		SilentParameters parameters = SilentParameters.builder(
				Collections.singleton("User.Read"),
				account).build();

		CompletableFuture<IAuthenticationResult> future = app.acquireTokenSilently(parameters);
		IAuthenticationResult updatedResult = future.get();

		//update session with latest token cache
		AzureSessionManagementHelper.setTokenCache(httpRequest, app.tokenCache().serialize());

		return updatedResult;
	}

	private void validateNonce(StateData stateData, String nonce) throws Exception {
		if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {

			SlackNotificationService.notifySlackChannel(
					AzureSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce",
					this.getClass().getSimpleName(),
					"validateNonce(StateData stateData, String nonce)",
					String.format("Statedata: %s, nonce: %s", stateData, nonce));

			throw new Exception(AzureSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce");
		}
	}

	private String getNonceClaimValueFromIdToken(String idToken) throws ParseException {
		return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("nonce");
	}

	private IAuthenticationResult getAuthResultByAuthCode(
			HttpServletRequest httpServletRequest,
			String domain,
			AuthorizationCode authorizationCode,
			String currentUri) throws Throwable {

		IAuthenticationResult result;
		ConfidentialClientApplication app;
		try {
			app = createClientApplication(httpServletRequest, domain);

			String authCode = authorizationCode.getValue();
			AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
					authCode,
					new URI(currentUri)).
					build();

			Future<IAuthenticationResult> future = app.acquireToken(parameters);

			result = future.get();
		} catch (ExecutionException e) {
			throw e.getCause();
		}

		if (result == null) {
			throw new ServiceUnavailableException("authentication result was null");
		}

		AzureSessionManagementHelper.setTokenCache(httpServletRequest, app.tokenCache().serialize());

		return result;
	}

	private ConfidentialClientApplication createClientApplication(HttpServletRequest httpRequest, String domain
			) throws IOException {
		
		AzureADConnectionProperties azureADConnectionProperties = new AzureADConnectionProperties(ExternalIAMSSOSuppliers.configurationForDomain(domain));
		return ConfidentialClientApplication.builder(azureADConnectionProperties.getClientId(),
				ClientCredentialFactory.createFromSecret(azureADConnectionProperties.getClientSecret())).
				authority(getAuthorityURL(domain)).
				build();
	}
	
	private String getAuthorityURL(String domain) {
		if(domain==null || domain.isEmpty()) {
			domain = "common";
		}
		return AAD_AUTHORITY_URL.replaceFirst("/*$", "") + "/" + domain;
	}
	
	private ConfidentialClientApplication createClientApplicationSilentFlow(HttpServletRequest httpRequest
			) throws IOException {
		String domain = AzureSessionManagementHelper.getSessionSelectedDomain(httpRequest);
		AzureADConnectionProperties azureADConnectionProperties = new AzureADConnectionProperties(ExternalIAMSSOSuppliers.configurationForDomain(domain));
		return ConfidentialClientApplication.builder(azureADConnectionProperties.getClientId(),
				ClientCredentialFactory.createFromSecret(azureADConnectionProperties.getClientSecret())).
				authority(getAuthorityURL(domain)).
				build();
	}

	private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
		if (oidcResponse.getIDToken() != null || oidcResponse.getAccessToken() != null ||
				oidcResponse.getAuthorizationCode() == null) {

			SlackNotificationService.notifySlackChannel(
					AzureSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received",
					this.getClass().getSimpleName(),
					"validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse)",
					String.format("AuthenticationSuccessResponse: %s", ToStringHelper.toString(oidcResponse)));

			throw new Exception(AzureSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received");
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
