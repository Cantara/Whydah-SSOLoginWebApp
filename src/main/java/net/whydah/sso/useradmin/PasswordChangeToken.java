package net.whydah.sso.useradmin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordChangeToken {
    private final String user;
    private final long timeout;
    private final String token;

    public PasswordChangeToken(String token) {
        this.token = token;
        String decoded = new String(Base64.getMimeDecoder().decode(token),
                StandardCharsets.ISO_8859_1);
        String[] elements = decoded.split(":");
        user = elements[0];
        timeout = Long.parseLong(elements[1]);
    }

    public boolean isValid() {
        return timeout > System.currentTimeMillis();
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "PasswordChangeToken{" +
                "user='" + user + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
