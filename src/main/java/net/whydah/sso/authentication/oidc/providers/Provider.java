package net.whydah.sso.authentication.oidc.providers;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface Provider {
	
    String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;
}
