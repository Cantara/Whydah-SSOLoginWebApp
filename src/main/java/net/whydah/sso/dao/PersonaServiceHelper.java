package net.whydah.sso.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import net.whydah.sso.model.PersonaCredential;

public class PersonaServiceHelper {
	
	private final static Logger log = LoggerFactory.getLogger(PersonaServiceHelper.class);
	private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
;
	private String personascredentials = "";
	
	public PersonaServiceHelper(Properties pros) throws IOException {
        personascredentials = pros.getProperty("personascredentials");
	}

	
	public List<PersonaCredential> getPersonasCredentials() {
		List<PersonaCredential> list = new ArrayList<PersonaCredential>();
		try {
			log.info("read personas from the source: " + personascredentials);
			if(personascredentials.startsWith("[")) {

				list = mapper.readValue(personascredentials, new TypeReference<List<PersonaCredential>>() {});
			} else {
				Client client = ClientBuilder.newClient();

				WebTarget webResource = client.target(personascredentials);
				Response response = webResource.request("application/json").get(Response.class);

				if (response.getStatus() != 200) {
					throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
				}

				String output = response.readEntity(String.class);
				list = mapper.readValue(output, new TypeReference<List<PersonaCredential>>() {});
			}

		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return list;
	}

}
