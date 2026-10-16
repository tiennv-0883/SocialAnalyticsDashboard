package com.example.socialanalytics.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.test.context.ActiveProfiles;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.entity.User;
import com.example.socialanalytics.repository.OAuthTokenRepository;
import com.example.socialanalytics.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import({ JpaOAuth2AuthorizedClientService.class, JpaOAuth2AuthorizedClientServiceTest.Registrations.class })
class JpaOAuth2AuthorizedClientServiceTest {

	@Autowired
	private JpaOAuth2AuthorizedClientService service;

	@Autowired
	private ClientRegistrationRepository registrations;

	@Autowired
	private OAuthTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	@Test
	void savesLoadsUpdatesAndRemovesTokens() {
		User user = new User();
		user.setUsername("tranb");
		user.setProvider(SocialPlatform.TWITTER);
		user.setProviderUserId("tw-9");
		userRepository.save(user);
		TestingAuthenticationToken principal = new TestingAuthenticationToken("tw-9", null);

		service.saveAuthorizedClient(client("access-1", "refresh-1"), principal);
		OAuth2AuthorizedClient loaded = service.loadAuthorizedClient("twitter", "tw-9");

		assertThat(loaded.getAccessToken().getTokenValue()).isEqualTo("access-1");
		assertThat(loaded.getAccessToken().getExpiresAt()).isEqualTo(now.plusSeconds(7200));
		assertThat(loaded.getAccessToken().getScopes()).containsExactlyInAnyOrder("users.read", "tweet.read");
		assertThat(loaded.getRefreshToken().getTokenValue()).isEqualTo("refresh-1");
		assertThat(tokenRepository.findAll()).singleElement()
			.satisfies(token -> assertThat(token.getUser().getId()).isEqualTo(user.getId()));

		service.saveAuthorizedClient(client("access-2", null), principal);
		assertThat(tokenRepository.count()).as("same user + provider is updated, not duplicated").isEqualTo(1);
		assertThat(service.loadAuthorizedClient("twitter", "tw-9").getRefreshToken()).isNull();

		service.removeAuthorizedClient("twitter", "tw-9");
		assertThat((Object) service.loadAuthorizedClient("twitter", "tw-9")).isNull();
	}

	@Test
	void returnsNullForUnknownRegistration() {
		assertThat((Object) service.loadAuthorizedClient("google", "x")).isNull();
	}

	private OAuth2AuthorizedClient client(String accessToken, String refreshToken) {
		return new OAuth2AuthorizedClient(registrations.findByRegistrationId("twitter"), "tw-9",
				new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, now, now.plusSeconds(7200),
						Set.of("users.read", "tweet.read")),
				refreshToken == null ? null : new OAuth2RefreshToken(refreshToken, now));
	}

	@TestConfiguration
	static class Registrations {

		@Bean
		ClientRegistrationRepository clientRegistrationRepository() {
			return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("twitter")
				.clientId("id")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.authorizationUri("https://x.com/i/oauth2/authorize")
				.tokenUri("https://api.x.com/2/oauth2/token")
				.build());
		}

	}

}
