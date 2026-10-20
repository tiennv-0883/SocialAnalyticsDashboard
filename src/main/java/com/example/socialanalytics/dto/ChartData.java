package com.example.socialanalytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.socialanalytics.entity.SocialPlatform;

public record ChartData(SocialPlatform platform, int days, String zone, List<String> labels,
		Map<String, List<Long>> series, Map<SocialPlatform, Totals> byPlatform, Instant generatedAt) {

	public record Totals(int posts, long likes, long shares, long comments, long followers) {
	}

}
