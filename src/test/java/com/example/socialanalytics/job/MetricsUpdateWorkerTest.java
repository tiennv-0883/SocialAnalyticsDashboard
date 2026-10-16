package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

class MetricsUpdateWorkerTest {

	private final SocialApiClient client = mock(SocialApiClient.class);

	private final PostRepository postRepository = mock(PostRepository.class);

	private final SocialMetricRepository metricRepository = mock(SocialMetricRepository.class);

	private MetricsUpdateWorker worker() {
		when(client.platform()).thenReturn(SocialPlatform.FACEBOOK);
		return new MetricsUpdateWorker(List.of(client), postRepository, metricRepository);
	}

	@Test
	void savesNewSnapshotBasedOnPreviousOne() {
		MetricsUpdateWorker worker = worker();
		givenPost(1L, "fb-1");
		SocialMetric previous = new SocialMetric();
		previous.setLikes(10);
		previous.setFollowers(500);
		previous.setCollectedAt(Instant.now());
		when(metricRepository.findTopByPostIdOrderByCollectedAtDescIdDesc(1L)).thenReturn(Optional.of(previous));
		when(client.fetchMetrics("fb-1", new MetricSnapshot(10, 0, 0, 500))).thenReturn(new MetricSnapshot(15, 1, 2, 510));

		ChunkResult result = worker.update(1L, SocialPlatform.FACEBOOK, List.of(1L));

		assertThat(result.updated()).isEqualTo(1);
		ArgumentCaptor<SocialMetric> saved = ArgumentCaptor.forClass(SocialMetric.class);
		verify(metricRepository).save(saved.capture());
		assertThat(saved.getValue().getLikes()).isEqualTo(15);
		assertThat(saved.getValue().getFollowers()).isEqualTo(510);
		assertThat(saved.getValue().getCollectedAt()).isNotNull();
	}

	@Test
	void oneFailingPostDoesNotStopTheChunk() {
		MetricsUpdateWorker worker = worker();
		givenPost(1L, "fb-1");
		givenPost(2L, "fb-2");
		givenPost(3L, "fb-3");
		when(client.fetchMetrics(any(), any())).thenReturn(new MetricSnapshot(1, 1, 1, 1));
		when(client.fetchMetrics(eq("fb-2"), any())).thenThrow(new SocialApiException("rate limited"));

		ChunkResult result = worker.update(1L, SocialPlatform.FACEBOOK, List.of(1L, 2L, 3L, 99L));

		assertThat(result.updated()).isEqualTo(2);
		assertThat(result.errors()).containsExactly("post 2: rate limited", "post 99: post deleted");
		verify(metricRepository, times(2)).save(any());
	}

	@Test
	void platformWithoutClientFailsItsPosts() {
		MetricsUpdateWorker worker = worker();

		ChunkResult result = worker.update(1L, SocialPlatform.TWITTER, List.of(5L));

		assertThat(result.failed()).isEqualTo(1);
		assertThat(result.errors().get(0)).contains("No API client for TWITTER");
	}

	private void givenPost(Long id, String externalId) {
		Post post = new Post();
		post.setId(id);
		post.setExternalId(externalId);
		post.setPlatform(SocialPlatform.FACEBOOK);
		when(postRepository.findById(id)).thenReturn(Optional.of(post));
	}

}
