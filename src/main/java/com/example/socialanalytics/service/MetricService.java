package com.example.socialanalytics.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.dto.MetricRequest;
import com.example.socialanalytics.dto.MetricResponse;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.exception.ResourceNotFoundException;
import com.example.socialanalytics.repository.SocialMetricRepository;
import com.example.socialanalytics.util.DtoMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricService {

	private final SocialMetricRepository metricRepository;

	private final PostService postService;

	public Page<MetricResponse> findAll(Long postId, Pageable pageable) {
		Page<SocialMetric> page = (postId == null) ? metricRepository.findAll(pageable)
				: metricRepository.findByPostId(postId, pageable);
		return page.map(DtoMapper::toResponse);
	}

	public MetricResponse findById(Long id) {
		return DtoMapper.toResponse(getMetric(id));
	}

	@Transactional
	public MetricResponse create(MetricRequest request) {
		SocialMetric metric = new SocialMetric();
		apply(request, metric);
		return DtoMapper.toResponse(metricRepository.save(metric));
	}

	@Transactional
	public MetricResponse update(Long id, MetricRequest request) {
		SocialMetric metric = getMetric(id);
		apply(request, metric);
		return DtoMapper.toResponse(metric);
	}

	@Transactional
	public void delete(Long id) {
		metricRepository.delete(getMetric(id));
	}

	private void apply(MetricRequest request, SocialMetric metric) {
		metric.setPost(postService.getPost(request.postId()));
		metric.setLikes(request.likes());
		metric.setShares(request.shares());
		metric.setComments(request.comments());
		metric.setFollowers(request.followers());
		metric.setCollectedAt(request.collectedAt() != null ? request.collectedAt() : Instant.now());
	}

	private SocialMetric getMetric(Long id) {
		return metricRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SocialMetric", id));
	}

}
