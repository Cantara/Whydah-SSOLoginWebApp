// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package net.whydah.sso.authentication.iamproviders.azuread;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.map.IMap;
import com.microsoft.aad.msal4j.IAccount;

import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.slack.SlackNotificationService;
import net.whydah.sso.utils.HazelcastMapHelper;

/**
 * Helpers for managing session
 */
public class AzureSessionManagementHelper {
	private final static Logger log = LoggerFactory.getLogger(AzureSessionManagementHelper.class);

	private static final Integer STATE_TTL = 86400;
	
	private static boolean byPassRemoval=true;
    static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Azure AD Authorization service - ";
	private static IMap<String, AzureSessionData> sessions = HazelcastMapHelper.register("sso_aad_auth_sessions");
	private static IMap<String, StateData> states = HazelcastMapHelper.register("sso_aad_auth_states");
	
	
	public static int countValidSessions() {
		removeExpiredSessions();
		return sessions.size();
	}
	
	public static int countStates() {
		return states.size();
	}
	
    public static AzureSessionData getSessionData(HttpServletRequest httpRequest) {	
    	List<String> removedSessionIds = removeExpiredSessions();
    	
		String sessionid = httpRequest.getSession().getId();
		if (sessions.containsKey(sessionid)) {
			return sessions.get(sessionid);
		} else {
			sessionid = SessionCookieHelper.getSessionCookie("aad", httpRequest); 
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				return sessions.get(sessionid);
			}
		}
		
		if(!removedSessionIds.contains(httpRequest.getSession().getId())) {
			sessions.put(httpRequest.getSession().getId(), new AzureSessionData());
			return sessions.get(httpRequest.getSession().getId());
		} else {
			return null;
		}
	}
    
    static StateData validateState(HttpSession session, String state) throws Exception {
		log.info("validateState - entry");
    	if (StringUtils.isNotEmpty(state)) {
			log.info("validateState - state not empty");
			StateData stateDataInSession = removeStateFromSession(session, state);
			log.info("validateState - removeStateFromSession ");

			if (stateDataInSession != null) {
				log.info("stateDataInSession != null:" + stateDataInSession);
				return stateDataInSession;
			}
		}
		log.info("failing - state is null:" + state);

    	SlackNotificationService.notifySlackChannel(
				AzureSessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "could not validate state",
				AzureSessionManagementHelper.class.getSimpleName(),
				"validateState(HttpSession session, String state)",
				String.format("state: %s", state));

    	throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate state");
    }
    
    
    
    private static StateData removeStateFromSession(HttpSession session, String state) {
    	log.debug("entry - removeStateFromSession:" + state);
		Iterator<Map.Entry<String, StateData>> it = states.entrySet().iterator();
		
		Date currTime = new Date();
		while (it.hasNext()) {
            Map.Entry<String, StateData> entry = it.next();
            long diffInSeconds = TimeUnit.MILLISECONDS.
                    toSeconds(currTime.getTime() - entry.getValue().getExpirationDate().getTime());

            if (diffInSeconds > STATE_TTL) {
            	states.remove(entry.getKey());
            }
        }
		
		if(byPassRemoval) {
			return states.get(state);
		}
		return states.remove(state);
	}
  
    
    public static void removeSession(HttpServletRequest httpRequest, HttpServletResponse response) {
		//remove cookie
		String sessionid = SessionCookieHelper.getSessionCookie("aad", httpRequest);
		if(sessionid!=null) {
			sessions.remove(sessionid);
			SessionCookieHelper.clearSessionCookie("aad", httpRequest, response);
		}
		sessions.remove(httpRequest.getSession().getId());
	}

	static void storeStateAndNonceInSession(HttpSession session, String domain, String state, String nonce, String redirectUri) {
		log.debug("storeStateAndNonceInSession: state {}, nonce {}, redirectUri {}", state, nonce, redirectUri);
		states.put(state, new StateData(domain, state, nonce, redirectUri, new Date()));
	}


    static void setTokenCache(HttpServletRequest httpRequest, String tokenCache){
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		log.debug("setTokenCache:" + sessiondata);
		sessiondata.setAadTokenCache(tokenCache);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
    }
    
    static boolean hasSession(HttpServletRequest httpRequest) {
        return getSessionData(httpRequest)!=null;
    }
    
    static String getTokenCache(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getAadTokenCache();
	}
    
    static void setSessionSelectedDomain(HttpServletRequest httpRequest, String selectedDomain) {
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setDomain(selectedDomain);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
    }

    static String getSessionSelectedDomain(HttpServletRequest httpRequest) {
    	return String.valueOf(getSessionData(httpRequest).getDomain());
    }

    
    static void setAccessToken(HttpServletRequest httpRequest, String data) {
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setAccessToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
    
    static String getAccessToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getAccessToken();
	}
    
    static void setIdToken(HttpServletRequest httpRequest, String data) {
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setIdToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
    
    static String getIdToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getIdToken();
	}
    
    static void setAccount(HttpServletRequest httpRequest, IAccount data) {
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setAccount(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
    
    static IAccount getAccount(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getAccount();
	}
    
    static void setExpiresOnDate(HttpServletRequest httpRequest, Date data) {
    	AzureSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setExpiresOnDate(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
    
    static Date getExpiresOnDate(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getExpiresOnDate();
	}
    
    
    public static List<String> removeExpiredSessions() {
		List<String> removedSessionIds = new ArrayList<String>();
		Iterator<Map.Entry<String, AzureSessionData>> it = sessions.entrySet().iterator();
		Date currTime = new Date();
		while (it.hasNext()) {
			Map.Entry<String, AzureSessionData> entry = it.next();
			if (entry.getValue().getEndOfUserTokenLifeMs()!=null && entry.getValue().getEndOfUserTokenLifeMs()!=0 && currTime.getTime() > entry.getValue().getEndOfUserTokenLifeMs()) {
				removedSessionIds.add(entry.getKey());
				sessions.remove(entry.getKey());
			}
		}
		return removedSessionIds;
	}

	
	public static void updateLifeSpanForSession(HttpServletRequest httpRequest, Long endOfTokenLifeMs) {
		
		String sessionid = httpRequest.getSession().getId();
		//try detecting current session first
		if(sessions.containsKey(sessionid)) {
			AzureSessionData session = sessions.get(sessionid);
			session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
			sessions.put(sessionid, session);
		} else {
			//try detecting sessionid in cookie
			sessionid = SessionCookieHelper.getSessionCookie("aad", httpRequest); 
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				AzureSessionData session = sessions.get(sessionid);
				session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
				sessions.put(sessionid, session);
			}
		}
	}
}
