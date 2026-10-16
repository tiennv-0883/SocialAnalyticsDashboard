package com.example.socialanalytics.security;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.entity.User;
import com.example.socialanalytics.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Loads the profile from Facebook / X after login, normalizes it to the same attribute
 * names ({@code id, name, username, email, provider, userId}) and creates or updates the
 * matching {@link User} row.
 */
@Slf4j
@Service
public class SocialOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	public static final String NAME_ATTRIBUTE = "id";

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

	private final UserRepository userRepository;

	@Autowired
	public SocialOAuth2UserService(UserRepository userRepository) {
		this(new DefaultOAuth2UserService(), userRepository);
	}

	SocialOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate, UserRepository userRepository) {
		this.delegate = delegate;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		Profile profile = Profile.from(registrationId, delegate.loadUser(userRequest).getAttributes());
		User user = upsert(profile);
		log.info("Social login: provider={} providerUserId={} userId={}", profile.provider(), profile.id(), user.getId());

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put(NAME_ATTRIBUTE, profile.id());
		attributes.put("name", profile.name());
		attributes.put("username", profile.username());
		attributes.put("email", profile.email());
		attributes.put("provider", profile.provider().name());
		attributes.put("userId", user.getId());
		return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, NAME_ATTRIBUTE);
	}

	private User upsert(Profile profile) {
		User user = userRepository.findByProviderAndProviderUserId(profile.provider(), profile.id()).orElseGet(() -> {
			User created = new User();
			created.setProvider(profile.provider());
			created.setProviderUserId(profile.id());
			return created;
		});
		user.setUsername(truncate(profile.username() != null ? profile.username() : profile.id(), 100));
		user.setDisplayName(truncate(profile.name(), 255));
		if (profile.email() != null) {
			user.setEmail(truncate(profile.email(), 255));
		}
		user.setLastLoginAt(Instant.now());
		return userRepository.save(user);
	}

	private static String truncate(String value, int max) {
		return (value == null || value.length() <= max) ? value : value.substring(0, max);
	}

	record Profile(SocialPlatform provider, String id, String name, String username, String email) {

		static Profile from(String registrationId, Map<String, Object> attributes) {
			SocialPlatform provider = switch (registrationId.toLowerCase(Locale.ROOT)) {
				case "facebook" -> SocialPlatform.FACEBOOK;
				case "twitter" -> SocialPlatform.TWITTER;
				default -> throw new OAuth2AuthenticationException(
						new OAuth2Error("unsupported_provider", "Unsupported provider: " + registrationId, null));
			};
			if (provider == SocialPlatform.TWITTER) {
				// X returns {"data": {"id", "name", "username"}}; email is not provided
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) attributes.get("data");
				if (data == null) {
					throw new OAuth2AuthenticationException(
							new OAuth2Error("invalid_user_info", "X user info has no 'data' field", null));
				}
				return new Profile(provider, string(data.get("id")), string(data.get("name")),
						string(data.get("username")), null);
			}
			String name = string(attributes.get("name"));
			return new Profile(provider, string(attributes.get("id")), name, name, string(attributes.get("email")));
		}

		private static String string(Object value) {
			return value == null ? null : value.toString();
		}

	}

}
