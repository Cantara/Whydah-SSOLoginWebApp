package net.whydah.sso.utils;

import lombok.Data;
import net.whydah.sso.util.StringConv;

@Data
public class Response {

	private int responseCode;
	private byte[] data = null;
	
	public String getContent() {
		if(data!=null) {
			return StringConv.UTF8(data);
		}
		return null;
	}
}
