package net.whydah.sso.authentication.iamproviders.whydah;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.whydah.sso.model.WhydahProvider;

public class WhydahOauthIntegrationConfig {
	
	Map<String, WhydahProvider> PROVIDERS;
	private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	
	public WhydahOauthIntegrationConfig(Properties properties) throws JsonMappingException, JsonProcessingException {
		String integrationConfig = properties.getProperty("whydah.integration.providers");
		PROVIDERS = mapper.readValue(integrationConfig, new TypeReference<List<WhydahProvider>>() {}).stream().distinct().collect(Collectors.toMap(WhydahProvider::getProvider, Function.identity()));
	}

	public Map<String, WhydahProvider> getProviderMap() {
		return PROVIDERS;
	}
	

}
