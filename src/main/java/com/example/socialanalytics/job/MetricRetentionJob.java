package com.example.socialanalytics.job;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.socialanalytics.repository.SocialMetricRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricRetentionJob {

	private final SocialMetricRepository metricRepository;

	private final RetentionProperties properties;

	@Scheduled(cron = "${app.jobs.metrics-retention.cron}")
	public void purgeOldMetrics() {
		purge();
	}

	public int purge() {
		Instant cutoff = Instant.now().minus(Duration.ofDays(properties.days()));
		int total = 0;
		List<Long> ids;
		do {
			ids = metricRepository.findPurgeableIds(cutoff, PageRequest.of(0, properties.batchSize()));
			if (!ids.isEmpty()) {
				metricRepository.deleteAllByIdInBatch(ids);
				total += ids.size();
			}
		}
		while (ids.size() == properties.batchSize());
		log.info("Metric retention: deleted {} snapshots older than {} days (latest snapshot of each post kept)",
				total, properties.days());
		return total;
	}

}
