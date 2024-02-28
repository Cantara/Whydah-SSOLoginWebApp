// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package net.whydah.sso.authentication.iamproviders.google;

import com.hazelcast.map.IMap;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.utils.HazelcastMapHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Helpers for managing session
 */
public class GoogleSessionManagementHelper {

	private final static Logger log = LoggerFactory.getLogger(GoogleSessionManagementHelper.class);

	private static final Integer STATE_TTL = 86400;

	private static boolean byPassRemoval = true;
	private static IMap<String, GoogleSessionData> sessions = HazelcastMapHelper.register("sso_google_auth_sessions");
	private static IMap<String, StateData> states = HazelcastMapHelper.register("sso_google_auth_states");

	static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Google Authorization service - ";

	public static int countValidSessions() {
		removeExpiredSessions();
		return sessions.size();
	}
	
	public static int countStates() {
		return states.size();
	}
	
	public static GoogleSessionData getSessionData(HttpServletRequest httpRequest) {
		
		List<String> removedSessionIds = removeExpiredSessions();
		
		String sessionid = httpRequest.getSession().getId();
		if (sessions.containsKey(sessionid)) {
			return sessions.get(sessionid);
		} else {
			sessionid = SessionCookieHelper.getSessionCookie("google", httpRequest); 
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				return sessions.get(sessionid);
			}
		}
		
		if(!removedSessionIds.contains(httpRequest.getSession().getId())) {
			sessions.put(httpRequest.getSession().getId(), new GoogleSessionData());
			return sessions.get(httpRequest.getSession().getId());
		} else {
			return null;
		}
	}

	static StateData validateState(HttpSession session, String state) throws Exception {
		try {
			log.debug("entry - validateState");
			if (StringUtils.isNotEmpty(state)) {
				log.debug(" - StringUtils.isNotEmpty(state):" + state);
				StateData stateDataInSession = removeStateFromSession(session, state);
				if (stateDataInSession != null) {
					log.debug(" - stateDataInSession != null:" + stateDataInSession);
					return stateDataInSession;
				}
			}
			log.debug(" - stateDataInSession == null:" + state);

		} catch (Exception e) {
			log.error("Exception in validateState", e);
		}

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
			if (!byPassRemoval) {
				return states.remove(state);
			} else {
				return states.get(state);
			}
	}

	public static void removeSession(HttpServletRequest httpRequest, HttpServletResponse response) {
		//remove cookie
		String sessionid = SessionCookieHelper.getSessionCookie("google", httpRequest);
		if (sessionid != null) {
			sessions.remove(sessionid);
			SessionCookieHelper.clearSessionCookie("google", httpRequest, response);
		}
		sessions.remove(httpRequest.getSession().getId());
	}

	static void storeStateAndNonceInSession(HttpSession session, String domain, String state, String nonce, String redirectUri) {
		states.put(state, new StateData(domain, state, nonce, redirectUri, new Date()));
	}

	static void setAccessToken(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setAccessToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setRefreshToken(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setRefreshToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setExpiryTimeSeconds(HttpServletRequest httpRequest, Long data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setExpiryTimeSeconds(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setEmail(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setEmail(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setFirstName(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setFirstName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setLastName(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setLastName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setSubject(HttpServletRequest httpRequest, String data) {
		GoogleSessionData sessiondata = getSessionData(httpRequest);
		sessiondata.setSubject(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static String getAccessToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getAccessToken();
	}

	static boolean hasSession(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest)!=null;
	}

	static String getRefreshToken(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getRefreshToken();
	}

	static Long getExpiryTimeSeconds(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getExpiryTimeSeconds();
	}

	static String getEmail(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getEmail();
	}

	static String getFirstName(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getFirstName();
	}

	static String getLastName(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getLastName();
	}

	static String getSubject(HttpServletRequest httpRequest) {
		return getSessionData(httpRequest).getSubject();
	}
	
	public static List<String> removeExpiredSessions() {
		List<String> removedSessionIds = new ArrayList<String>();
		Iterator<Map.Entry<String, GoogleSessionData>> it = sessions.entrySet().iterator();
		Date currTime = new Date();
		while (it.hasNext()) {
			Map.Entry<String, GoogleSessionData> entry = it.next();
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
			GoogleSessionData session = sessions.get(sessionid);
			session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
			sessions.put(sessionid, session);
		} else {
			//try detecting sessionid in cookie
			sessionid = SessionCookieHelper.getSessionCookie("google", httpRequest); 
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				GoogleSessionData session = sessions.get(sessionid);
				session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
				sessions.put(sessionid, session);
			}
		}
	}


}
