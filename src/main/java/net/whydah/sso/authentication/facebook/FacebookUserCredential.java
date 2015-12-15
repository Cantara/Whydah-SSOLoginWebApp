package net.whydah.sso.authentication.facebook;

import net.whydah.sso.authentication.UserCredential;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 3/10/12
 */
@Deprecated //  Use UserCredential in Typelib
public class FacebookUserCredential implements UserCredential {
    private final String fbId;
    private final String username;

    public FacebookUserCredential(String facebookUserId, String username) {
        if (facebookUserId == null) {
            throw new IllegalArgumentException("facebookUserId cannot be null.");
        }
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null.");
        }

        this.fbId = facebookUserId;
        this.username = username;
    }

    @Override
    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                "<usercredential>\n" +
                "    <params>\n" +
                "        <fbId>" + fbId + "</fbId>\n" +
                "        <username>" + username + "</username>\n" +
                "    </params> \n" +
                "</usercredential>\n";
    }

    @Override
    public String toString() {
        return "FacebookUserCredential{" +
                "fbId='" + fbId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
