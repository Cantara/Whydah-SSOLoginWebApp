package net.whydah.sso.authentication.oidc;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Provider {
    void logout(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException;
    String provider();
}
