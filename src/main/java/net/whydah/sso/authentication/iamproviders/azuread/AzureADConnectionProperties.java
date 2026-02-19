package net.whydah.sso.authentication.iamproviders.azuread;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whydah.sso.config.AppConfig;

public class AzureADConnectionProperties {

	private final static Logger log = LoggerFactory.getLogger(AzureADConnectionProperties.class);

	private String clientId;
	private String clientSecret;

	public AzureADConnectionProperties(Properties properties) throws IOException {

		try {
			if(properties!=null) {
				clientSecret = properties.getProperty("aad.secretKey");
				clientId = properties.getProperty("aad.clientId");
			}
		} catch (Exception e) {
			log.warn("Unable to find domain specific Azure AD config, using shared common.", e);
		}

		if (clientSecret == null || clientId == null) {

			Properties appConfig = AppConfig.readProperties();
			clientSecret = appConfig.getProperty("aad.secretKey");
			clientId = appConfig.getProperty("aad.clientId");

		}

	}


	public String getClientId() {
		return clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}



}
