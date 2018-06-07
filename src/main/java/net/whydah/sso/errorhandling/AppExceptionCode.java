package net.whydah.sso.errorhandling;

import org.springframework.http.HttpStatus;


public class AppExceptionCode {

	//USER EXCEPTIONS
	public static AppException USER_USERTOKEN_NOT_FOUND_4000 = new AppException(HttpStatus.NOT_FOUND, 4000, "Usertoken not found.", "user token is null", "");
	public static AppException USER_INVALID_PHONE_NUMBER_4001 = new AppException(HttpStatus.BAD_REQUEST, 4001, "Illegal phone number.", "A valid phone number must be at least 8 digits.", "");
	public static AppException USER_INVALID_PARAMETERS_4002 = new AppException(HttpStatus.BAD_REQUEST, 4002, "Invalid parameters.", "", "");
	public static AppException USER_MISSING_PARAMETERS_4003 = new AppException(HttpStatus.BAD_REQUEST, 4003, "Missing parameters.", "", "");
	public static AppException USER_INVALID_USER_4004 = new AppException(HttpStatus.BAD_REQUEST, 4004, "Illegal user token.", "", "");
	public static AppException USER_OPPLYSNINGEN_LOOKUP_NOT_FOUND_4005 = new AppException(HttpStatus.NOT_FOUND, 4005, "Opplysningen data cannot be found for this user.", "getOpplysningenData lookup result failed.", "");
	//public static AppException USER_CRM_RECORD_NOT_FOUND_4006 = new AppException(HttpStatus.NOT_FOUND, 4006, "no CRMRecord found.", "no CRMRecord found. Check getCrmdata()", "");
	public static AppException USER_CRMSERVICE_FAILURE_4006 = new AppException(HttpStatus.SERVICE_UNAVAILABLE, 4006, "Failure occured when serving crm data. Server is temporarily down or too busy to handle the request.", "", "");
	public static AppException USER_INVALID_CRM_REFERENCE_4007 = new AppException(HttpStatus.BAD_REQUEST, 4004, "Illegal CRM reference found in user token.", "", "");
	
	
	//APPLICATION EXCEPTIONS
	public static AppException APP_NOT_FOUND_5000 = new AppException(HttpStatus.BAD_REQUEST, 5000, "Illegal Application Session.", "Application is invalid", "");
	
	//ADDRESS EXCEPTIONS
	public static AppException ADDRESS_DEFAULT_NOT_SET_6000 = new AppException(HttpStatus.BAD_REQUEST, 6000, "Default address has not been set.", "Default address has not been set.", "");
	public static AppException ADDRESS_SELECTION_FAILED_6001 = new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 6001, "Cannot save selection of the current address.", "updateRoleEntry() failed.", "");
	public static AppException ADDRESS_NOT_FOUND_6002 = new AppException(HttpStatus.NOT_FOUND, 6002, "The requested address label is not existing.", "The requested address label is not existing.", "");
	public static AppException ADDRESS_UPDATE_CRM_DELIVERY_ADDRESS_FAILED_6003 = new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 6003, "Cannot update changes for this delivery address.", "updateDeliveryAddress() failed.", "");
	public static AppException ADDRESS_WRONG_JSON_FORMAT_6004 = new AppException(HttpStatus.BAD_REQUEST, 6004, "Wrong json format.", "", "");
	public static AppException ADDRESS_DELETE_FAILED_6005 = new AppException(HttpStatus.INTERNAL_SERVER_ERROR, 6005, "Cannot delete the selected address.", "updateRoleEntry() failed.", "");
	
			
			
	
	
	
	
}
