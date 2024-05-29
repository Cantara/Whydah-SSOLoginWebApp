package net.whydah.sso.authentication.oidc.providers;

import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Provider {
	
    String login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String authenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmBasicInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;

    String confirmExist(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Model model) throws Throwable;
}
