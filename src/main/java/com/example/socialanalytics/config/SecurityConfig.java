package com.example.socialanalytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.example.socialanalytics.security.CsrfCookieFilter;
import com.example.socialanalytics.security.LoggingAuthenticationFailureHandler;
import com.example.socialanalytics.security.SocialOAuth2UserService;

@Configuration
public class SecurityConfig {

	private static final RequestMatcher API = new OrRequestMatcher(
			PathPatternRequestMatcher.pathPattern("/posts/**"), PathPatternRequestMatcher.pathPattern("/metrics/**"),
			PathPatternRequestMatcher.pathPattern("/import-posts"),
			PathPatternRequestMatcher.pathPattern("/export-report"), PathPatternRequestMatcher.pathPattern("/jobs/**"),
			PathPatternRequestMatcher.pathPattern("/chart-data"), PathPatternRequestMatcher.pathPattern("/ws/**"),
			PathPatternRequestMatcher.pathPattern("/exchange-rate"), PathPatternRequestMatcher.pathPattern("/export/**"),
			PathPatternRequestMatcher.pathPattern("/export"));

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SocialOAuth2UserService userService)
			throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/login", "/error", "/favicon.ico", "/css/**", "/js/**", "/swagger-ui.html", "/swagger-ui/**",
					"/v3/api-docs/**", "/soap/**")
			.permitAll()
			.anyRequest()
			.authenticated())
			.oauth2Login(oauth2 -> oauth2.loginPage("/login")
				.defaultSuccessUrl("/dashboard", true)
				.failureHandler(new LoggingAuthenticationFailureHandler())
				.userInfoEndpoint(userInfo -> userInfo.userService(userService)))
			.logout(logout -> logout.logoutSuccessUrl("/login?logout"))
			.csrf(csrf -> csrf.spa().ignoringRequestMatchers("/ws/**", "/soap/**"))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()));
		return http.build();
	}

	private static AuthenticationEntryPoint authenticationEntryPoint() {
		return DelegatingAuthenticationEntryPoint.builder()
			.addEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API)
			.defaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
			.build();
	}

}
