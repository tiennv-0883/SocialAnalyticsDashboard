package com.example.socialanalytics.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "platform_stats")
public class PlatformStats extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 20)
	private SocialPlatform platform;

	@Column(name = "post_count", nullable = false)
	private long postCount;

	@Column(name = "imported_posts", nullable = false)
	private long importedPosts;

	@Column(name = "import_count", nullable = false)
	private long importCount;

	@Column(name = "last_import_at")
	private Instant lastImportAt;

}
