package com.example.socialanalytics.util;

import com.example.socialanalytics.dto.MetricResponse;
import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.dto.PostResponse;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;

public final class DtoMapper {

	private DtoMapper() {
	}

	public static PostResponse toResponse(Post post) {
		return new PostResponse(post.getId(), post.getPlatform(), post.getExternalId(), post.getUrl(),
				post.getTitle(), post.getContent(), post.getCreatedAt(), post.getUpdatedAt());
	}

	public static void copyToEntity(PostRequest request, Post post) {
		post.setPlatform(request.platform());
		post.setExternalId(request.externalId());
		post.setUrl(request.url());
		post.setTitle(request.title());
		post.setContent(request.content());
	}

	public static MetricResponse toResponse(SocialMetric metric) {
		return new MetricResponse(metric.getId(), metric.getPost().getId(), metric.getLikes(), metric.getShares(),
				metric.getComments(), metric.getFollowers(), metric.getCollectedAt());
	}

}
