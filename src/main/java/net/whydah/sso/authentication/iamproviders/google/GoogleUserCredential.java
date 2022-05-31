package net.whydah.sso.authentication.iamproviders.google;

import net.whydah.sso.user.types.UserCredential;

public class GoogleUserCredential extends UserCredential {

	private String aadId;

	public GoogleUserCredential(String aadId, String username, String password) {
		super(username, password);
		this.aadId = aadId;
	}

	@Override
	public String toXML() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
				"<usercredential>\n" +
				"    <params>\n" +
				"        <aadId>" + aadId + "</aadId>\n" +
				"        <username>" + getUserName() + "</username>\n" +
				"        <password>" + getPassword() + "</password>\n" +
				"    </params> \n" +
				"</usercredential>\n";
	}

	@Override
	public String toString() {
		return "GoogleUserCredential{" +
				"aadId='" + aadId + '\'' +
				", username='" + getUserName() + '\'' +
				", password='" + getPassword() + '\'' +
				'}';
	}

}
