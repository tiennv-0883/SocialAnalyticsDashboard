package com.example.socialanalytics.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.entity.User;
import com.example.socialanalytics.repository.UserRepository;

class SocialOAuth2UserServiceTest {

	@SuppressWarnings("unchecked")
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mock(OAuth2UserService.class);

	private final UserRepository userRepository = mock(UserRepository.class);

	private final SocialOAuth2UserService service = new SocialOAuth2UserService(delegate, userRepository);

	@BeforeEach
	void saveAssignsId() {
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			if (user.getId() == null) {
				user.setId(7L);
			}
			return user;
		});
	}

	@Test
	void createsUserFromFacebookProfile() {
		givenProviderReturns(Map.of("id", "fb-123", "name", "Nguyen Van A", "email", "a@example.com"), "id");
		when(userRepository.findByProviderAndProviderUserId(SocialPlatform.FACEBOOK, "fb-123")).thenReturn(Optional.empty());

		OAuth2User user = service.loadUser(request("facebook"));

		assertThat(user.getName()).isEqualTo("fb-123");
		assertThat((String) user.getAttribute("email")).isEqualTo("a@example.com");
		assertThat((String) user.getAttribute("provider")).isEqualTo("FACEBOOK");
		assertThat((Long) user.getAttribute("userId")).isEqualTo(7L);
		assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
	}

	@Test
	void flattensXProfileAndUpdatesExistingUser() {
		givenProviderReturns(Map.of("data", Map.of("id", "tw-9", "name", "Tran B", "username", "tranb")), "data");
		User existing = new User();
		existing.setId(3L);
		existing.setProvider(SocialPlatform.TWITTER);
		existing.setProviderUserId("tw-9");
		existing.setEmail("kept@example.com");
		when(userRepository.findByProviderAndProviderUserId(SocialPlatform.TWITTER, "tw-9")).thenReturn(Optional.of(existing));
		Instant before = Instant.now();

		OAuth2User user = service.loadUser(request("twitter"));

		assertThat(user.getName()).isEqualTo("tw-9");
		assertThat((String) user.getAttribute("username")).isEqualTo("tranb");
		assertThat((Long) user.getAttribute("userId")).isEqualTo(3L);
		assertThat(existing.getUsername()).isEqualTo("tranb");
		assertThat(existing.getDisplayName()).isEqualTo("Tran B");
		assertThat(existing.getEmail()).as("X sends no email; keep the stored one").isEqualTo("kept@example.com");
		assertThat(existing.getLastLoginAt()).isAfterOrEqualTo(before);
	}

	@Test
	void rejectsUnknownProvider() {
		givenProviderReturns(Map.of("id", "g-1"), "id");

		assertThatThrownBy(() -> service.loadUser(request("google"))).isInstanceOf(OAuth2AuthenticationException.class);
	}

	private void givenProviderReturns(Map<String, Object> attributes, String nameAttribute) {
		when(delegate.loadUser(any())).thenReturn(
				new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, nameAttribute));
	}

	private static OAuth2UserRequest request(String registrationId) {
		ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
			.clientId("id")
			.clientSecret("secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri("https://example.com/auth")
			.tokenUri("https://example.com/token")
			.userInfoUri("https://example.com/me")
			.userNameAttributeName("id")
			.build();
		OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(),
				Instant.now().plusSeconds(3600));
		return new OAuth2UserRequest(registration, token);
	}

}
