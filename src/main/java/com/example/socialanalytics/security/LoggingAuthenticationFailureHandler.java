package com.example.socialanalytics.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs why a social login failed (token exchange, user info, state mismatch...) and
 * redirects to {@code /login?error}; the exception is kept in the session so the login
 * page can show it.
 */
@Slf4j
public class LoggingAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	public LoggingAuthenticationFailureHandler() {
		super("/login?error");
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String code = (exception instanceof OAuth2AuthenticationException oauth2) ? oauth2.getError().getErrorCode()
				: exception.getClass().getSimpleName();
		log.warn("Social login failed at {}: [{}] {}", request.getRequestURI(), code, exception.getMessage(), exception);
		super.onAuthenticationFailure(request, response, exception);
	}

}
