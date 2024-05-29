package net.whydah.sso.authentication.iamproviders;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WhydahProvider {
	
	private String displayText;
	private String provider;
	private boolean enabled;
	private String oauthClientId;
	private String oauthUrl;
	private String logo;
	

	@Override
	public boolean equals(Object o) {
	    // self check
	    if (this == o)
	        return true;
	    // null check
	    if (o == null)
	        return false;
	    // type check and cast
	    if (getClass() != o.getClass())
	        return false;
	    WhydahProvider obj = (WhydahProvider) o;
	    // field comparison
	    return Objects.equals(provider, obj.provider);
	}
}
