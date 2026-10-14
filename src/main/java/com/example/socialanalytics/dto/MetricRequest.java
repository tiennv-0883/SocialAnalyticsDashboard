package com.example.socialanalytics.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MetricRequest(
		@NotNull Long postId,
		@PositiveOrZero long likes,
		@PositiveOrZero long shares,
		@PositiveOrZero long comments,
		@PositiveOrZero long followers,
		Instant collectedAt) {
}
