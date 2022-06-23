package net.whydah.sso.useradmin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.whydah.sso.commands.adminapi.user.CommandGetUser;
import net.whydah.sso.commands.adminapi.user.CommandUpdateUser;
import net.whydah.sso.config.AppConfig;
import net.whydah.sso.dao.SessionDao;
import net.whydah.sso.errorhandling.AppException;
import net.whydah.sso.errorhandling.AppExceptionCode;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserIdentity;
import net.whydah.sso.util.StringConv;

@Controller
public class IntegrationController {

	private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);
	
	private URI uasServiceUri;
	
	public IntegrationController() throws IOException {
		uasServiceUri = UriBuilder.fromUri(AppConfig.readProperties().getProperty("useradminservice")).build();
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/integration/user/{uid}/", method = RequestMethod.PUT)
	public String updateUserIdentity(
			@PathVariable("uid") String uid,
			HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {
		log.trace("updateUserIdentity with uid={}", uid);

		accessCheck(request);
		
		String json = readInput(request.getInputStream());
		CommandUpdateUser cmd = new CommandUpdateUser(uasServiceUri, 
				SessionDao.instance.getServiceClient().getMyAppTokenID(), 
				SessionDao.instance.getUserAdminToken().getUserTokenId(), 
				uid, json);
		return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());

	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/integration/user/{uid}", method = RequestMethod.GET)
	public String getUserIdentity(
			@PathVariable("uid") String uid,
			HttpServletRequest request, HttpServletResponse response, Model model) throws AppException, IOException {
		log.trace("getUserIdentity with uid={}", uid);
		
		accessCheck(request);
		
		CommandGetUser cmd = new CommandGetUser(uasServiceUri, 
				SessionDao.instance.getServiceClient().getMyAppTokenID(), 
				SessionDao.instance.getUserAdminToken().getUserTokenId(), 
				uid);
		return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/integration/user/{uid}/{personref}", method = RequestMethod.PUT)
	public String updatePersonRef(
			@PathVariable("uid") String uid,
			@PathVariable("personref") String personRef,
			HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {
		log.trace("updateUserIdentity with uid={}", uid);
		accessCheck(request);
		
		String adminUserTokenId = SessionDao.instance.getUserAdminToken().getUserTokenId();
		if(adminUserTokenId==null) {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Failed connection to the backend server", "", "");
		}
		String apptokenId = SessionDao.instance.getServiceClient().getMyAppTokenID();
		if(apptokenId==null) {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Failed to get apptokenId", "", "");
		}
		CommandGetUser cmd = new CommandGetUser(uasServiceUri, 
				apptokenId, 
				adminUserTokenId, 
				uid);
		String json = cmd.execute();
		if(json!=null) {
			UserIdentity user = UserIdentityMapper.fromUserIdentityJson(cmd.execute());
			user.setPersonRef(personRef);

			CommandUpdateUser updatecmd = new CommandUpdateUser(uasServiceUri, 
					SessionDao.instance.getServiceClient().getMyAppTokenID(), 
					SessionDao.instance.getUserAdminToken().getUserTokenId(), 
					uid, json);
			return handleResponse(response, model, updatecmd.execute(), updatecmd.getResponseBodyAsByteArray(), updatecmd.getStatusCode());
		} else {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Failed to get user", "", "");
		}
	}
	
	private String handleResponse(HttpServletResponse response, Model model, String result, byte[] raw_response,
			int statusCode) throws AppException {
		if (result != null) {
            model.addAttribute("jsondata", result.replace("\\'", "'").replace("\'", "'"));
		}

		/*
		 * else { String responseMsg = raw_response!=null?
		 * StringConv.UTF8(raw_response): "N/A" ;
		 * log.warn("Failed connection to UAS. Response code: {} - Response message: {}"
		 * , statusCode, responseMsg); String msg =
		 * "{\"error\":\"Failed connection to the backend server - " + (statusCode!=0 ?
		 * ("Status code: " + statusCode) : "A fallback exception occured." ) + "\"}";
		 * model.addAttribute(JSON_DATA_KEY, msg);
		 * 
		 * if(statusCode!=0) { response.setStatus(statusCode); } else {
		 * response.setStatus(500); } }
		 */

		else {
			String responseMsg = raw_response != null ? StringConv.UTF8(raw_response) : "N/A";
			log.warn("Failed connection to UAS. Response code: {} - Response message: {}", statusCode, responseMsg);

			throw new AppException(statusCode != 0 ? HttpStatus.valueOf(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR,
					9999, "Failed connection to the backend server. Response message: " + responseMsg, "", "");
		}

		response.setContentType("application/json; charset=utf-8");
		return "json";
	}
	
	String readInput(InputStream inputStream) throws IOException {
		 ByteArrayOutputStream result = new ByteArrayOutputStream();
		 byte[] buffer = new byte[1024];
		 for (int length; (length = inputStream.read(buffer)) != -1; ) {
		     result.write(buffer, 0, length);
		 }
		 // StandardCharsets.UTF_8.name() > JDK 7
		 return result.toString("UTF-8");
	}
	
	private void accessCheck(HttpServletRequest request) throws AppException, IOException  {
		//security check
		String authorization = request.getHeader("Authorization");
		if(authorization==null) {
			throw new AppException(HttpStatus.BAD_REQUEST, 9998, "No authorization header", "","" );
		}
		String token = authorization.substring(7);
		log.info("checking access secret {} against the config");
		String secretKey = AppConfig.readProperties().getProperty("integration_secret_key");
		if(!token.equals(secretKey)) {
			log.error("not matched with {}", secretKey);
			throw new AppException(HttpStatus.BAD_REQUEST, 9998, "Access denied", "","" );
		}
		log.info("access granted");
	}
}
