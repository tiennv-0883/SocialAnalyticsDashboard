package com.example.socialanalytics.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MetricsUpdateWorker {

	private final Map<SocialPlatform, SocialApiClient> clients = new EnumMap<>(SocialPlatform.class);

	private final PostRepository postRepository;

	private final SocialMetricRepository metricRepository;

	public MetricsUpdateWorker(List<SocialApiClient> clients, PostRepository postRepository,
			SocialMetricRepository metricRepository) {
		clients.forEach(client -> this.clients.put(client.platform(), client));
		this.postRepository = postRepository;
		this.metricRepository = metricRepository;
	}

	@Async(AsyncConfig.METRICS_EXECUTOR)
	public CompletableFuture<ChunkResult> updateChunk(long runId, SocialPlatform platform, List<Long> postIds) {
		return CompletableFuture.completedFuture(update(runId, platform, postIds));
	}

	/** Synchronous body of {@link #updateChunk}; one failing post never stops the others. */
	ChunkResult update(long runId, SocialPlatform platform, List<Long> postIds) {
		long start = System.currentTimeMillis();
		SocialApiClient client = clients.get(platform);
		int updated = 0;
		List<String> errors = new ArrayList<>();
		for (Long postId : postIds) {
			try {
				if (client == null) {
					throw new SocialApiException("No API client for " + platform);
				}
				updatePost(client, postId);
				updated++;
			}
			catch (RuntimeException ex) {
				log.warn("[job run {}] post {} ({}) failed: {}", runId, postId, platform, ex.getMessage());
				errors.add("post " + postId + ": " + ex.getMessage());
			}
		}
		log.debug("[job run {}] {} chunk of {} posts done in {}ms: {} updated, {} failed", runId, platform,
				postIds.size(), System.currentTimeMillis() - start, updated, errors.size());
		return new ChunkResult(updated, errors);
	}

	private void updatePost(SocialApiClient client, Long postId) {
		Post post = postRepository.findById(postId).orElseThrow(() -> new SocialApiException("post deleted"));
		MetricSnapshot previous = metricRepository.findTopByPostIdOrderByCollectedAtDescIdDesc(postId)
			.map(metric -> new MetricSnapshot(metric.getLikes(), metric.getShares(), metric.getComments(),
					metric.getFollowers()))
			.orElse(null);
		MetricSnapshot current = client.fetchMetrics(post.getExternalId(), previous);

		SocialMetric metric = new SocialMetric();
		metric.setPost(post);
		metric.setLikes(current.likes());
		metric.setShares(current.shares());
		metric.setComments(current.comments());
		metric.setFollowers(current.followers());
		metric.setCollectedAt(Instant.now());
		metricRepository.save(metric);
	}

}
