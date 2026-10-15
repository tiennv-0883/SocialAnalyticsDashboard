package com.example.socialanalytics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private SocialMetricRepository metricRepository;

	@Test
	void findsPostsByPlatformAndChecksExistence() {
		em.persist(post(SocialPlatform.FACEBOOK, "fb-1"));
		em.persist(post(SocialPlatform.TWITTER, "tw-1"));

		assertThat(postRepository.findByPlatform(SocialPlatform.TWITTER, PageRequest.of(0, 10)))
			.extracting(Post::getExternalId)
			.containsExactly("tw-1");
		assertThat(postRepository.existsByPlatformAndExternalId(SocialPlatform.FACEBOOK, "fb-1")).isTrue();
		assertThat(postRepository.existsByPlatformAndExternalId(SocialPlatform.TWITTER, "fb-1")).isFalse();
	}

	@Test
	void findLatestPerPostReturnsNewestSnapshotOfEachPost() {
		Post first = em.persist(post(SocialPlatform.FACEBOOK, "fb-1"));
		Post second = em.persist(post(SocialPlatform.TWITTER, "tw-1"));
		em.persist(metric(first, 10, "2026-10-01T00:00:00Z"));
		em.persist(metric(first, 20, "2026-10-02T00:00:00Z"));
		em.persist(metric(second, 5, "2026-10-01T00:00:00Z"));
		em.flush();

		assertThat(metricRepository.findLatestPerPost()).extracting(SocialMetric::getLikes)
			.containsExactlyInAnyOrder(20L, 5L);
	}

	@Test
	void deleteByPostIdRemovesOnlyThatPostsMetrics() {
		Post first = em.persist(post(SocialPlatform.FACEBOOK, "fb-1"));
		Post second = em.persist(post(SocialPlatform.TWITTER, "tw-1"));
		em.persist(metric(first, 1, "2026-10-01T00:00:00Z"));
		em.persist(metric(second, 2, "2026-10-01T00:00:00Z"));

		metricRepository.deleteByPostId(first.getId());

		assertThat(metricRepository.findAll()).extracting(SocialMetric::getLikes).containsExactly(2L);
	}

	private static Post post(SocialPlatform platform, String externalId) {
		Post post = new Post();
		post.setPlatform(platform);
		post.setExternalId(externalId);
		post.setUrl("https://example.com/" + externalId);
		return post;
	}

	private static SocialMetric metric(Post post, long likes, String collectedAt) {
		SocialMetric metric = new SocialMetric();
		metric.setPost(post);
		metric.setLikes(likes);
		metric.setCollectedAt(Instant.parse(collectedAt));
		return metric;
	}

}
