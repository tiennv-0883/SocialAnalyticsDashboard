package com.example.socialanalytics.job;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.jobs.update-metrics")
public record JobProperties(@NotBlank String cron, @Min(1) int chunkSize, @Min(1) int corePoolSize,
		@Min(1) int maxPoolSize, @Min(0) int queueCapacity) {
}
