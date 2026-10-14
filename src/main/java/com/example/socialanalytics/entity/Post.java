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
 * A social post being tracked (usually imported from an Excel file).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "posts",
		uniqueConstraints = @UniqueConstraint(columnNames = { "platform", "external_id" }))
public class Post extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialPlatform platform;

	@Column(name = "external_id", nullable = false, length = 100)
	private String externalId;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String content;

}
