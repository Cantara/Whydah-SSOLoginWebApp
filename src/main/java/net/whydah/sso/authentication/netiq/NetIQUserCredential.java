package net.whydah.sso.authentication.netiq;

import net.whydah.sso.authentication.UserCredential;

@Deprecated //  Use UserCredential in Typelib
public class NetIQUserCredential implements UserCredential {
    private final String netiqId;
    private final String username;

    public NetIQUserCredential(String netIQInstance, String username) {
        if (netIQInstance == null) {
            throw new IllegalArgumentException("netIQInstance cannot be null.");
        }
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null.");
        }

        this.netiqId = netIQInstance;
        this.username = username;
    }

    @Override
    public String toXML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                "<usercredential>\n" +
                "    <params>\n" +
                "        <netiqId>" + netiqId + "</netiqId>\n" +
                "        <username>" + username + "</username>\n" +
                "    </params> \n" +
                "</usercredential>\n";
    }

    @Override
    public String toString() {
        return "NetIQUserCredential{" +
                "netiqId='" + netiqId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}

