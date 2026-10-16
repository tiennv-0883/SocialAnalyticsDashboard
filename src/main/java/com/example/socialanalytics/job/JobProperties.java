package com.example.socialanalytics.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jobs.update-metrics")
public record JobProperties(String cron, int chunkSize, int corePoolSize, int maxPoolSize, int queueCapacity) {
}
