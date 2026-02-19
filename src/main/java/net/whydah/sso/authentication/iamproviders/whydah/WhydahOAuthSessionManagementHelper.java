// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package net.whydah.sso.authentication.iamproviders.whydah;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.map.IMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.whydah.sso.authentication.iamproviders.SessionCookieHelper;
import net.whydah.sso.authentication.iamproviders.StateData;
import net.whydah.sso.utils.HazelcastMapHelper;

/**
 * Helpers for managing session
 */
public class WhydahOAuthSessionManagementHelper {

	private final static Logger log = LoggerFactory.getLogger(WhydahOAuthSessionManagementHelper.class);

	private static final Integer STATE_TTL = 86400;

	private static boolean byPassRemoval = true;
	private static IMap<String, WhydahOauthSessionData> sessions = HazelcastMapHelper.register("sso_whydah_auth_sessions");
	private static IMap<String, StateData> states = HazelcastMapHelper.register("sso_whydah_auth_states");

	static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Google Authorization service - ";

	public static int countValidSessions() {
		removeExpiredSessions();
		return sessions.size();
	}
	
	public static int countStates() {
		return states.size();
	}
	
	public static WhydahOauthSessionData getSessionData(HttpServletRequest httpRequest, String provider) {
		
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
			sessions.put(httpRequest.getSession().getId(), new WhydahOauthSessionData());
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

	public static void removeSession(HttpServletRequest httpRequest, HttpServletResponse response, String provider) {
		//remove cookie
		String sessionid = SessionCookieHelper.getSessionCookie(provider, httpRequest);
		if (sessionid != null) {
			sessions.remove(sessionid);
			SessionCookieHelper.clearSessionCookie(provider, httpRequest, response);
		}
		sessions.remove(httpRequest.getSession().getId());
	}

	static void storeStateAndNonceInSession(HttpSession session, String domain, String state, String nonce, String redirectUri) {
		states.put(state, new StateData(domain, state, nonce, redirectUri, new Date()));
	}

	static void setAccessToken(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setAccessToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setRefreshToken(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setRefreshToken(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setExpiryTimeSeconds(HttpServletRequest httpRequest, String provider, Long data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setExpiryTimeSeconds(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setEmail(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setEmail(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setCellPhone(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setCellPhone(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
	
	static void setFirstName(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setFirstName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setLastName(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setLastName(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static void setSubject(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setSubject(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
	
	static void setSecurityLevel(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setSecurityLevel(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
	
	static void setPersonRef(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setPersonRef(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}
	
	static void setUID(HttpServletRequest httpRequest, String provider, String data) {
		WhydahOauthSessionData sessiondata = getSessionData(httpRequest, provider);
		sessiondata.setUid(data);
		sessions.put(httpRequest.getSession().getId(), sessiondata);
	}

	static String getAccessToken(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getAccessToken();
	}

	static boolean hasSession(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider)!=null;
	}

	static String getRefreshToken(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getRefreshToken();
	}

	static Long getExpiryTimeSeconds(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getExpiryTimeSeconds();
	}

	static String getEmail(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getEmail();
	}

	static String getFirstName(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getFirstName();
	}

	static String getLastName(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getLastName();
	}

	static String getSubject(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getSubject();
	}
	
	static String getSecurityLevel(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getSecurityLevel();
	}
	
	static String getPersonRef(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getPersonRef();
	}
	
	static String getCellPhone(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getCellPhone();
	}
	
	static String getUID(HttpServletRequest httpRequest, String provider) {
		return getSessionData(httpRequest, provider).getUid();
	}
	
	
	public static List<String> removeExpiredSessions() {
		List<String> removedSessionIds = new ArrayList<String>();
		Iterator<Map.Entry<String, WhydahOauthSessionData>> it = sessions.entrySet().iterator();
		Date currTime = new Date();
		while (it.hasNext()) {
			Map.Entry<String, WhydahOauthSessionData> entry = it.next();
			if (entry.getValue().getEndOfUserTokenLifeMs()!=null && entry.getValue().getEndOfUserTokenLifeMs()!=0 && currTime.getTime() > entry.getValue().getEndOfUserTokenLifeMs()) {
				removedSessionIds.add(entry.getKey());
				sessions.remove(entry.getKey());
			}
		}
		return removedSessionIds;
	}

	
	public static void updateLifeSpanForSession(HttpServletRequest httpRequest, Long endOfTokenLifeMs, String provider) {
		
		String sessionid = httpRequest.getSession().getId();
		//try detecting current session first
		if(sessions.containsKey(sessionid)) {
			WhydahOauthSessionData session = sessions.get(sessionid);
			session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
			sessions.put(sessionid, session);
		} else {
			//try detecting sessionid in cookie
			sessionid = SessionCookieHelper.getSessionCookie(provider, httpRequest); 
			if(sessionid!=null && sessions.containsKey(sessionid)) {
				WhydahOauthSessionData session = sessions.get(sessionid);
				session.setEndOfUserTokenLifeMs(endOfTokenLifeMs);
				sessions.put(sessionid, session);
			}
		}
	}


}
