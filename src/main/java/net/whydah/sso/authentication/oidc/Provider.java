package net.whydah.sso.authentication.oidc;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface Provider {
    void logout(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException;
    String provider();
}
