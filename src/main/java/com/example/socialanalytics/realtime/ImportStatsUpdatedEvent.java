package com.example.socialanalytics.realtime;

import java.time.Instant;

public record ImportStatsUpdatedEvent(String importId, String fileName, int imported, int failed, Instant occurredAt) {
}
