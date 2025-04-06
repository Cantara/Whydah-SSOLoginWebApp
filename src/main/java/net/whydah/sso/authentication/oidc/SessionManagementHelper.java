// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package net.whydah.sso.authentication.oidc;

import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.utils.HazelcastMapHelper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.map.IMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Helpers for managing session
 */
public class SessionManagementHelper {

	private final static Logger log = LoggerFactory.getLogger(SessionManagementHelper.class);

	private static final Integer STATE_TTL = 86400;

	private static boolean byPassRemoval = true;
	private IMap<String, SessionData> sessions;
	private IMap<String, StateData> states;

	private final String failedToValidateMessage;
	private final String provider;

	public SessionManagementHelper(String provider) {
		this.provider = provider;
		failedToValidateMessage = "Failed to validate data received from Authorization service - " + provider;
		sessions = HazelcastMapHelper.register(provider + "sso_oidc_auth_sessions");
		states = HazelcastMapHelper.register(provider + "sso_oidc_auth_states");
		
	}

	public int countValidSessions() {
		removeExpiredSessions();
		return sessions.size();
	}
	
	public  int countStates() {
		return states.size();
	}

	public String getFailedToValidateMessage() {
		return failedToValidateMessage;
	}

	public SessionData getSessionData(HttpServletRequest httpRequest) {
		
		List<String> removedSessionIds = removeExpiredSessions();
		
		String sessionid = httpRequest.getSession().getId();
		if (sessions.containsKey(sessionid)) {
			return sessions.get(sessionid);
		} else {
			sessionid = SessionCookieHelper.getSessionCookie(provider, httpRequest);
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				return sessions.get(sessionid);
			}
		}
		
		if(!removedSessionIds.contains(httpRequest.getSession().getId())) {
			sessions.put(httpRequest.getSession().getId(), new SessionData());
			return sessions.get(httpRequest.getSession().getId());
		} else {
			return null;
		}
	}

	StateData validateState(HttpSession session, String state) throws Exception {
		try {
			log.debug("entry - validateState");
			if (StringUtils.isNotEmpty(state)) {
				log.debug(" - StringUtils.isNotEmpty(state):" + state);
				StateData stateDataInSession = removeStateFromStates(state);
				if (stateDataInSession != null) {
					log.debug(" - stateDataInSession != null:" + stateDataInSession);
					return stateDataInSession;
				}
			}
			log.debug(" - stateDataInSession == null:" + state);

		} catch (Exception e) {
			log.error("Exception in validateState", e);
		}

		throw new Exception(failedToValidateMessage + " - could not validate state");
	}


	private  StateData removeStateFromStates(String state) {
		log.debug("entry - removeStateFromStates:" + state);
		
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
			if (!byPassRemoval) {
				return states.remove(state);
			} else {
				return states.get(state);
			}
	}

	public void removeSession(HttpServletRequest httpRequest, HttpServletResponse response) {
		//remove cookie
		String sessionid = SessionCookieHelper.getSessionCookie(provider, httpRequest);
		if (sessionid != null) {
			sessions.remove(sessionid);
			SessionCookieHelper.clearSessionCookie(provider, httpRequest, response);
		}
		sessions.remove(httpRequest.getSession().getId());
	}

	 void storeStateAndNonceInStates(String state, String nonce, String redirectUri) {
		states.put(state, new StateData(state, nonce, redirectUri, new Date()));
	}

	void setAccessToken(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setAccessToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setRefreshToken(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setRefreshToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setExpiryTimeSeconds(HttpServletRequest httpRequest, Long data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setExpiryTimeSeconds(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setEmail(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setEmail(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setPhoneNumber(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setPhoneNumber(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setFirstName(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setFirstName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setLastName(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setLastName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	void setSubject(HttpServletRequest httpRequest, String data) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setSubject(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	String getAccessToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getAccessToken();
	}

	boolean hasSession(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest)!=null;
	}

	String getRefreshToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getRefreshToken();
	}

	Long getExpiryTimeSeconds(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getExpiryTimeSeconds();
	}

	String getEmail(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getEmail();
	}

	String getPhoneNumber(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getPhoneNumber();
	}

	String getFirstName(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getFirstName();
	}

	String getLastName(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getLastName();
	}

	String getSubject(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getSubject();
	}

	public List<String> removeExpiredSessions() {
		List<String> removedSessionIds = new ArrayList<String>();
		Iterator<Map.Entry<String, SessionData>> it = sessions.entrySet().iterator();
		Date currTime = new Date();
		while (it.hasNext()) {
			Map.Entry<String, SessionData> entry = it.next();
			if (entry.getValue().getEndOfUserTokenLifeMs()!=null && entry.getValue().getEndOfUserTokenLifeMs()!=0 && currTime.getTime() > entry.getValue().getEndOfUserTokenLifeMs()) {
				removedSessionIds.add(entry.getKey());
				sessions.remove(entry.getKey());
			}
		}
		return removedSessionIds;
	}

	String getUserInfoJsonstring(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getUserInfoJsonString();
	}
	
	Object getClaim(HttpServletRequest httpRequest, String claimName) {
		return getSessionData(httpRequest).getClaimsSet().get(claimName);
	}
	
	public void updateLifeSpanForSession(HttpServletRequest httpRequest, Long endOfTokenLifeMs) {
		
		String sessionid = httpRequest.getSession().getId();
		//try detecting current session first
		if(sessions.containsKey(sessionid)) {
			SessionData session = sessions.get(sessionid);
			session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
			sessions.put(sessionid, session);
		} else {
			//try detecting sessionid in cookie
			sessionid = SessionCookieHelper.getSessionCookie(provider, httpRequest);
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				SessionData session = sessions.get(sessionid);
				session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
				sessions.put(sessionid, session);
			}
		}
	}

	public void setUserInfoJsonString(HttpServletRequest httpRequest, String userinfoJsonString) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setUserInfoJsonString(userinfoJsonString);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
		
	}

	public void setClaimsSet(HttpServletRequest httpRequest, Map<String, Object> claimSets) {
		SessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setClaimsSet(claimSets);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
		
	}


}
