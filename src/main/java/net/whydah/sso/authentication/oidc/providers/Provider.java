package net.whydah.sso.authentication.oidc.providers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;



public interface Provider {
	
    String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;
}
