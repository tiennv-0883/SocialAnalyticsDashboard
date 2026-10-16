package com.example.socialanalytics.controller;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String login(HttpServletRequest request, Model model) {
		model.addAttribute("facebookLoginUrl", loginUrl(request, "facebook", "localhost"));
		model.addAttribute("twitterLoginUrl", loginUrl(request, "twitter", "127.0.0.1"));
		HttpSession session = request.getSession(false);
		if (request.getParameter("error") != null && session != null
				&& session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof AuthenticationException ex) {
			model.addAttribute("loginError", ex.getMessage());
		}
		return "login";
	}

	private static String loginUrl(HttpServletRequest request, String registrationId, String devHost) {
		String path = "/oauth2/authorization/" + registrationId;
		String host = request.getServerName();
		boolean isDevHost = "localhost".equals(host) || "127.0.0.1".equals(host);
		if (!isDevHost || devHost.equals(host)) {
			return path;
		}
		return UriComponentsBuilder.newInstance()
			.scheme(request.getScheme())
			.host(devHost)
			.port(request.getServerPort())
			.path(request.getContextPath() + path)
			.toUriString();
	}

}
