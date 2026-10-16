package com.example.socialanalytics.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTests {

	private static final String NEW_POST = """
			{"platform":"FACEBOOK","externalId":"sec-%s","url":"https://fb.com/sec"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void pagesRedirectAnonymousUsersToLogin() throws Exception {
		mockMvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void apiReturns401ForAnonymousUsers() throws Exception {
		mockMvc.perform(get("/posts")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/export-report")).andExpect(status().isUnauthorized());
	}

	@Test
	void loginPageAndApiDocsArePublic() throws Exception {
		mockMvc.perform(get("/login").header("Host", "localhost:8080"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("href=\"/oauth2/authorization/facebook\"")))
			.andExpect(content().string(containsString("http://127.0.0.1:8080/oauth2/authorization/twitter")));
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
	}

	@Test
	void twitterAuthorizationUsesPkce() throws Exception {
		// X rejects authorization requests without PKCE
		mockMvc.perform(get("/oauth2/authorization/twitter"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", startsWith("https://x.com/i/oauth2/authorize?")))
			.andExpect(header().string("Location", containsString("code_challenge=")))
			.andExpect(header().string("Location", containsString("code_challenge_method=S256")))
			.andExpect(header().string("Location", containsString("scope=users.read%20tweet.read")));
	}

	@Test
	void facebookAuthorizationRedirectsToFacebook() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/facebook"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", startsWith("https://www.facebook.com/dialog/oauth?")))
			.andExpect(header().string("Location", containsString("scope=public_profile%20email")));
	}

	@Test
	void stateChangingRequestsRequireCsrfToken() throws Exception {
		mockMvc.perform(post("/posts").with(oauth2Login()).contentType(MediaType.APPLICATION_JSON)
			.content(NEW_POST.formatted("no-token")))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/posts").with(oauth2Login()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(NEW_POST.formatted("with-token")))
			.andExpect(status().isCreated());
	}

	@Test
	void dashboardShowsUserAndCsrfProtectedForms() throws Exception {
		mockMvc.perform(get("/dashboard").with(oauth2Login().attributes(attributes -> {
			attributes.put("id", "123");
			attributes.put("name", "Test User");
			attributes.put("provider", "FACEBOOK");
		})))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Test User")))
			.andExpect(content().string(containsString("action=\"/logout\"")))
			.andExpect(content().string(containsString("action=\"/dashboard/import\"")))
			.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

	@Test
	void loginPageShowsLastLoginError() throws Exception {
		mockMvc.perform(get("/login").param("error", "").sessionAttr(
				WebAttributes.AUTHENTICATION_EXCEPTION,
				new BadCredentialsException("[invalid_token_response] boom")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("[invalid_token_response] boom")));
	}

	@Test
	void logoutRedirectsToLoginPage() throws Exception {
		mockMvc.perform(post("/logout").with(oauth2Login()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?logout"));
	}

}
