package com.example.socialanalytics.dto;

import java.time.Instant;

public record MetricResponse(
		Long id,
		Long postId,
		long likes,
		long shares,
		long comments,
		long followers,
		Instant collectedAt) {
}
