package com.example.socialanalytics.job;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import com.example.socialanalytics.entity.SocialPlatform;

public class MockSocialApiClient implements SocialApiClient {

	private final SocialPlatform platform;

	private final Duration latency;

	private final double failureRate;

	public MockSocialApiClient(SocialPlatform platform, Duration latency, double failureRate) {
		this.platform = platform;
		this.latency = latency;
		this.failureRate = failureRate;
	}

	@Override
	public SocialPlatform platform() {
		return platform;
	}

	@Override
	public MetricSnapshot fetchMetrics(String externalId, MetricSnapshot previous) {
		sleep();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (failureRate > 0 && random.nextDouble() < failureRate) {
			throw new SocialApiException(platform + " API temporarily unavailable (mock) for post " + externalId);
		}
		MetricSnapshot base = (previous != null) ? previous
				: new MetricSnapshot(random.nextLong(0, 500), random.nextLong(0, 50), random.nextLong(0, 100),
						random.nextLong(100, 5_000));
		return new MetricSnapshot(base.likes() + random.nextLong(0, 51), base.shares() + random.nextLong(0, 11),
				base.comments() + random.nextLong(0, 21), base.followers() + random.nextLong(0, 31));
	}

	private void sleep() {
		if (latency.isZero()) {
			return;
		}
		try {
			Thread.sleep(latency.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new SocialApiException(platform + " API call interrupted");
		}
	}

}
