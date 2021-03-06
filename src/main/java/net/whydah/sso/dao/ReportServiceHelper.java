package net.whydah.sso.dao;

import java.net.URI;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whydah.sso.authentication.whydah.clients.WhydahServiceClient;
import net.whydah.sso.commands.extensions.statistics.CommandGetUserSessionStats;
import net.whydah.sso.extensions.useractivity.helpers.UserActivityHelper;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;

public class ReportServiceHelper {


	private final static Logger log = LoggerFactory.getLogger(ReportServiceHelper.class);
	
	WhydahServiceClient serviceClient;
	URI reportservice;

	protected ReportServiceHelper(WhydahServiceClient serviceClient, URI reportservice){
		this.serviceClient = serviceClient;
		this.reportservice = reportservice;
	}

	public String getUserAccessLog(String userTokenId, Instant from, Instant to){
		String userTokenXml = serviceClient.getUserTokenByUserTokenID(userTokenId);
		String userid = UserTokenXpathHelper.getUserID(userTokenXml);
		String userActivitiesJson = new CommandGetUserSessionStats(reportservice, userid, from, to).execute();
		return UserActivityHelper.getTimedUserSessionsJsonFromUserActivityJson(userActivitiesJson, userid);
	}

	

//	 public void addUserActivities(Model model, String userTokenXml) {
//	        model.addAttribute(ConstantValue.USERACTIVITIES_SIMPLIFIED, "{}");
//	        if (UserTokenXpathHelper.getUserID(userTokenXml).length() > 2) {
//	            try {
//	                String userid = UserTokenXpathHelper.getUserID(userTokenXml);
//	                String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
//	                log.warn(">==================== 1 ");
//
//	                String userActivitiesJson = new CommandListUserActivities(reportservice, serviceClient.getMyAppTokenID(), userTokenId, userid).execute();
//	                //    model.addAttribute(ModelHelper.USERACTIVITIES, userActivitiesJson);
//	                log.warn(">==================== 2 ");
//                    model.addAttribute(ConstantValue.USERACTIVITIES_SIMPLIFIED, UserActivityHelper.getTimedUserSessionsJsonFromUserActivityJson(userActivitiesJson, userid));
//                    log.warn(">==================== 3 ");
//	            } catch (Exception e) {
//
//	            }
//	        }
//	    }

}
