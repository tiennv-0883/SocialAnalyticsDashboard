package com.example.socialanalytics.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.socialanalytics.entity.OAuthToken;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.OAuthTokenRepository;
import com.example.socialanalytics.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persists the tokens received at login in the {@code oauth_tokens} table
 * (replaces Spring's default in-memory storage, so tokens survive restarts).
 */
@Service
@RequiredArgsConstructor
public class JpaOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

	private final OAuthTokenRepository tokenRepository;

	private final UserRepository userRepository;

	private final ClientRegistrationRepository clientRegistrationRepository;

	@Override
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration == null) {
			return null;
		}
		return (T) tokenRepository.findByRegistrationIdAndPrincipalName(clientRegistrationId, principalName)
			.map(token -> toAuthorizedClient(registration, token))
			.orElse(null);
	}

	@Override
	@Transactional
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		String registrationId = authorizedClient.getClientRegistration().getRegistrationId();
		String principalName = principal.getName();
		OAuthToken token = tokenRepository.findByRegistrationIdAndPrincipalName(registrationId, principalName)
			.orElseGet(OAuthToken::new);
		token.setRegistrationId(registrationId);
		token.setPrincipalName(principalName);
		token.setUser(userRepository
			.findByProviderAndProviderUserId(SocialPlatform.valueOf(registrationId.toUpperCase()), principalName)
			.orElse(null));

		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		token.setAccessToken(accessToken.getTokenValue());
		token.setAccessTokenIssuedAt(accessToken.getIssuedAt());
		token.setAccessTokenExpiresAt(accessToken.getExpiresAt());
		token.setAccessTokenScopes(String.join(",", accessToken.getScopes()));

		OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
		token.setRefreshToken(refreshToken == null ? null : refreshToken.getTokenValue());
		token.setRefreshTokenIssuedAt(refreshToken == null ? null : refreshToken.getIssuedAt());
		tokenRepository.save(token);
	}

	@Override
	@Transactional
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		tokenRepository.deleteByRegistrationIdAndPrincipalName(clientRegistrationId, principalName);
	}

	private static OAuth2AuthorizedClient toAuthorizedClient(ClientRegistration registration, OAuthToken token) {
		Set<String> scopes = StringUtils.hasText(token.getAccessTokenScopes())
				? Arrays.stream(token.getAccessTokenScopes().split(",")).collect(Collectors.toSet()) : Set.of();
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
				token.getAccessToken(), token.getAccessTokenIssuedAt(), token.getAccessTokenExpiresAt(), scopes);
		OAuth2RefreshToken refreshToken = (token.getRefreshToken() == null) ? null
				: new OAuth2RefreshToken(token.getRefreshToken(), token.getRefreshTokenIssuedAt());
		return new OAuth2AuthorizedClient(registration, token.getPrincipalName(), accessToken, refreshToken);
	}

}
