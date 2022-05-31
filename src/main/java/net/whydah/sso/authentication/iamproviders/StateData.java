package net.whydah.sso.authentication.iamproviders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Date;

public class StateData implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String state;
    private String nonce;
    private String redirectURI; //whydah redirectURI
    private String domain;
    private Date expirationDate;
   

    public StateData(String nonce, String redirectURI, Date expirationDate) {
        this.nonce = nonce;
        this.expirationDate = expirationDate;
        this.redirectURI = redirectURI;
    }
    
    public StateData(String domain, String state, String nonce, String redirectURI, Date expirationDate) {
    	this.setDomain(domain);
        this.nonce = nonce;
        this.expirationDate = expirationDate;
        this.redirectURI = redirectURI;
        this.state = state;
    }

    public String getState() {
        return state;
    }

    
    public String getNonce() {
        return nonce;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

	public String getRedirectURI() {
		return redirectURI;
	}

	public void setRedirectURI(String redirectURI) {
		this.redirectURI = redirectURI;
	}
	
	

	@Override
	public int hashCode() {
	    return new HashCodeBuilder()
	            .append(state)
	            .append(nonce)
	            .append(redirectURI)
	            .toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj instanceof StateData) {
	        final StateData state = (StateData) obj;

			return new EqualsBuilder()
					.append(state, state.state)
					.append(nonce, state.nonce)
					.append(redirectURI, state.redirectURI)
					.append(domain, state.domain)
					.isEquals();
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "StateData{" +
				"state='" + state + '\'' +
				", nonce='" + nonce + '\'' +
				", redirectURI='" + redirectURI + '\'' +
				", domain='" + domain + '\'' +
				", expirationDate=" + expirationDate +
				'}';
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}