package net.whydah.sso.authentication.oidc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Provider {
    void logout(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException;
    String provider();
}
