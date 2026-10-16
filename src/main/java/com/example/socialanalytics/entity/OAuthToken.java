package com.example.socialanalytics.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oauth_tokens",
		uniqueConstraints = @UniqueConstraint(columnNames = { "registration_id", "principal_name" }))
public class OAuthToken extends BaseEntity {

	@Column(name = "registration_id", nullable = false, length = 50)
	private String registrationId;

	/** Provider user id (the OAuth2 principal name). */
	@Column(name = "principal_name", nullable = false, length = 100)
	private String principalName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
	private String accessToken;

	@Column(name = "access_token_issued_at")
	private Instant accessTokenIssuedAt;

	@Column(name = "access_token_expires_at")
	private Instant accessTokenExpiresAt;

	@Column(name = "access_token_scopes", length = 1000)
	private String accessTokenScopes;

	@Column(name = "refresh_token", columnDefinition = "TEXT")
	private String refreshToken;

	@Column(name = "refresh_token_issued_at")
	private Instant refreshTokenIssuedAt;

}
