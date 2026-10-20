package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.SocialMetricRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import({ MetricRetentionJob.class, MetricRetentionJobTest.Retention.class })
class MetricRetentionJobTest {

	@Autowired
	private MetricRetentionJob job;

	@Autowired
	private SocialMetricRepository metricRepository;

	@Autowired
	private TestEntityManager em;

	@Test
	void deletesOldSnapshotsButKeepsLatestOfEachPost() {
		Post active = post("active");
		SocialMetric activeOld1 = metric(active, 100);
		SocialMetric activeOld2 = metric(active, 60);
		SocialMetric activeRecent = metric(active, 10);
		Post inactive = post("inactive");
		SocialMetric inactiveOnly = metric(inactive, 200);
		Post stale = post("stale");
		SocialMetric staleOlder = metric(stale, 50);
		SocialMetric staleLatest = metric(stale, 40);
		em.flush();

		int deleted = job.purge();
		em.clear();

		assertThat(deleted).isEqualTo(3);
		assertThat(metricRepository.findAll()).extracting(SocialMetric::getId)
			.containsExactlyInAnyOrder(activeRecent.getId(), inactiveOnly.getId(), staleLatest.getId())
			.doesNotContain(activeOld1.getId(), activeOld2.getId(), staleOlder.getId());
	}

	@Test
	void nothingToDelete() {
		metric(post("fresh"), 1);
		em.flush();

		assertThat(job.purge()).isZero();
	}

	private Post post(String externalId) {
		Post post = new Post();
		post.setPlatform(SocialPlatform.FACEBOOK);
		post.setExternalId(externalId);
		post.setUrl("https://example.com/" + externalId);
		return em.persist(post);
	}

	private SocialMetric metric(Post post, int daysAgo) {
		SocialMetric metric = new SocialMetric();
		metric.setPost(post);
		metric.setCollectedAt(Instant.now().minus(Duration.ofDays(daysAgo)));
		return em.persist(metric);
	}

	@TestConfiguration
	static class Retention {

		@Bean
		RetentionProperties retentionProperties() {
			return new RetentionProperties("-", 30, 2);
		}

	}

}
