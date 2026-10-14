package com.example.socialanalytics.dto;

import java.time.Instant;

import com.example.socialanalytics.entity.SocialPlatform;

public record PostResponse(
		Long id,
		SocialPlatform platform,
		String externalId,
		String url,
		String title,
		String content,
		Instant createdAt,
		Instant updatedAt) {
}
