package net.whydah.sso.authentication.iamproviders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


public class WhydahOauthIntegrationConfig {
	
	Map<String, WhydahProvider> PROVIDERS;
	private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public WhydahOauthIntegrationConfig(Properties properties) {
		PROVIDERS = new HashMap<>();
		try {
			String integrationConfig = properties.getProperty("whydah.integration.providers");
			if (integrationConfig != null && !integrationConfig.trim().isEmpty()) {
				PROVIDERS = mapper.readValue(integrationConfig,
								new TypeReference<List<WhydahProvider>>() {})
						.stream()
						.filter(p -> p.getProvider() != null)
						.distinct()
						.collect(Collectors.toMap(WhydahProvider::getProvider, Function.identity()));
			}
		} catch (JsonProcessingException e) {
			// Log the error but don't fail the application
			System.err.println("Error parsing whydah.integration.providers: " + e.getMessage());
			// Provide a detailed error message that might help diagnose the issue
			System.err.println("Please check the JSON format in your properties file.");
		}
	}

	public Map<String, WhydahProvider> getProviderMap() {
		return PROVIDERS;
	}
	

}
