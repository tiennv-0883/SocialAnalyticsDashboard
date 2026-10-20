package com.example.socialanalytics.realtime;

import java.time.Instant;
import java.util.Map;

public record LiveNotification(String type, String message, Instant occurredAt, Map<String, Object> data) {

	public static final String METRICS_UPDATED = "METRICS_UPDATED";

	public static final String IMPORT_COMPLETED = "IMPORT_COMPLETED";

}
