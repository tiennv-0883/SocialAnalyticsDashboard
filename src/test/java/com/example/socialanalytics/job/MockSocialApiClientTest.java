package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.example.socialanalytics.entity.SocialPlatform;

class MockSocialApiClientTest {

	@Test
	void numbersNeverDecrease() {
		MockSocialApiClient client = new MockSocialApiClient(SocialPlatform.FACEBOOK, Duration.ZERO, 0);
		MetricSnapshot previous = new MetricSnapshot(100, 10, 20, 1_000);

		for (int i = 0; i < 50; i++) {
			MetricSnapshot next = client.fetchMetrics("fb-1", previous);
			assertThat(next.likes()).isGreaterThanOrEqualTo(previous.likes());
			assertThat(next.shares()).isGreaterThanOrEqualTo(previous.shares());
			assertThat(next.comments()).isGreaterThanOrEqualTo(previous.comments());
			assertThat(next.followers()).isGreaterThanOrEqualTo(previous.followers());
			previous = next;
		}
	}

	@Test
	void firstFetchStartsFromBaseline() {
		MetricSnapshot first = new MockSocialApiClient(SocialPlatform.TWITTER, Duration.ZERO, 0).fetchMetrics("tw", null);

		assertThat(first.followers()).isGreaterThanOrEqualTo(100);
	}

	@Test
	void failureRateOneAlwaysFails() {
		MockSocialApiClient client = new MockSocialApiClient(SocialPlatform.TWITTER, Duration.ZERO, 1.0);

		assertThatThrownBy(() -> client.fetchMetrics("tw-1", null)).isInstanceOf(SocialApiException.class)
			.hasMessageContaining("tw-1");
	}

}
