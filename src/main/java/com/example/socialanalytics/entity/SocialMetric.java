package com.example.socialanalytics.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshot of a post's engagement numbers at a point in time.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "social_metrics", indexes = { @Index(columnList = "post_id, collected_at"),
		@Index(name = "idx_social_metrics_collected_at", columnList = "collected_at") })
public class SocialMetric extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(nullable = false)
	private long likes;

	@Column(nullable = false)
	private long shares;

	@Column(nullable = false)
	private long comments;

	@Column(nullable = false)
	private long followers;

	@Column(name = "collected_at", nullable = false)
	private Instant collectedAt;

}
