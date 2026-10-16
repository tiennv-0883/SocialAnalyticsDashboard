package com.example.socialanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.socialanalytics.dto.MetricRequest;
import com.example.socialanalytics.dto.MetricResponse;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.exception.ResourceNotFoundException;
import com.example.socialanalytics.repository.SocialMetricRepository;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

	@Mock
	private SocialMetricRepository metricRepository;

	@Mock
	private PostService postService;

	@InjectMocks
	private MetricService metricService;

	private final Post post = PostServiceTest.post(1L, "fb-1");

	@Test
	void createUsesGivenCollectedAt() {
		Instant collectedAt = Instant.parse("2026-10-01T10:00:00Z");
		when(postService.getPost(1L)).thenReturn(post);
		when(metricRepository.save(any(SocialMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MetricResponse response = metricService.create(new MetricRequest(1L, 10, 2, 3, 100, collectedAt));

		assertThat(response.postId()).isEqualTo(1L);
		assertThat(response.likes()).isEqualTo(10);
		assertThat(response.followers()).isEqualTo(100);
		assertThat(response.collectedAt()).isEqualTo(collectedAt);
	}

	@Test
	void createDefaultsCollectedAtToNow() {
		when(postService.getPost(1L)).thenReturn(post);
		when(metricRepository.save(any(SocialMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		MetricResponse response = metricService.create(new MetricRequest(1L, 0, 0, 0, 0, null));

		assertThat(response.collectedAt()).isBetween(before, Instant.now());
	}

	@Test
	void createFailsWhenPostMissing() {
		when(postService.getPost(42L)).thenThrow(new ResourceNotFoundException("Post", 42L));

		assertThatThrownBy(() -> metricService.create(new MetricRequest(42L, 0, 0, 0, 0, null)))
			.isInstanceOf(ResourceNotFoundException.class);
		verify(metricRepository, never()).save(any());
	}

	@Test
	void findAllWithPostIdFiltersByPost() {
		Pageable pageable = PageRequest.of(0, 10);
		SocialMetric metric = metric(5L);
		when(metricRepository.findByPostId(1L, pageable)).thenReturn(new PageImpl<>(List.of(metric)));

		assertThat(metricService.findAll(1L, pageable).getContent()).extracting(MetricResponse::id).containsExactly(5L);
	}

	@Test
	void updateOverwritesValues() {
		SocialMetric metric = metric(5L);
		when(metricRepository.findById(5L)).thenReturn(Optional.of(metric));
		when(postService.getPost(1L)).thenReturn(post);

		MetricResponse response = metricService.update(5L, new MetricRequest(1L, 50, 6, 7, 800, null));

		assertThat(response.likes()).isEqualTo(50);
		assertThat(response.shares()).isEqualTo(6);
	}

	@Test
	void deleteThrowsWhenMissing() {
		when(metricRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> metricService.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	private SocialMetric metric(Long id) {
		SocialMetric metric = new SocialMetric();
		metric.setId(id);
		metric.setPost(post);
		metric.setCollectedAt(Instant.now());
		return metric;
	}

}
