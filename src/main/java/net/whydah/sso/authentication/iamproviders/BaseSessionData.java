package net.whydah.sso.authentication.iamproviders;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BaseSessionData implements Serializable {
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private String domain="common";
	private Long endOfUserTokenLifeMs = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli(); //default live one day if not set
	
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public Long getEndOfUserTokenLifeMs() {
		return endOfUserTokenLifeMs;
	}
	public void setEndOfUserTokenLifeMs(Long endOfUserTokenLifeMs) {
		this.endOfUserTokenLifeMs = endOfUserTokenLifeMs;
	}
}
