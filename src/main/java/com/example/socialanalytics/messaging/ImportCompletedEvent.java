package com.example.socialanalytics.messaging;

import java.time.Instant;
import java.util.Map;

import com.example.socialanalytics.entity.SocialPlatform;

public record ImportCompletedEvent(String importId, String fileName, int totalRows, int imported, int failed,
		Map<SocialPlatform, Integer> importedByPlatform, Instant occurredAt) {

	public static final String TYPE = "IMPORT_COMPLETED";

}
