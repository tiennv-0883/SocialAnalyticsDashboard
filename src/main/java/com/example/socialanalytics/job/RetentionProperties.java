package com.example.socialanalytics.job;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.jobs.metrics-retention")
public record RetentionProperties(@NotBlank String cron, @Min(1) int days, @Min(1) int batchSize) {
}
