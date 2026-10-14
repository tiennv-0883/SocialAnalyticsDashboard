package com.example.socialanalytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin user, logged in via a social provider (Facebook / Twitter).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users",
		uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_user_id" }))
public class User extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String username;

	@Column(length = 255)
	private String email;

	@Column(name = "display_name", length = 255)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialPlatform provider;

	@Column(name = "provider_user_id", nullable = false, length = 100)
	private String providerUserId;

}
