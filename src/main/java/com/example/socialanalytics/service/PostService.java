package com.example.socialanalytics.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.dto.PostResponse;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.exception.DuplicateResourceException;
import com.example.socialanalytics.exception.ResourceNotFoundException;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;
import com.example.socialanalytics.util.DtoMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private final PostRepository postRepository;

	private final SocialMetricRepository metricRepository;

	public Page<PostResponse> findAll(SocialPlatform platform, Pageable pageable) {
		Page<Post> page = (platform == null) ? postRepository.findAll(pageable)
				: postRepository.findByPlatform(platform, pageable);
		return page.map(DtoMapper::toResponse);
	}

	public PostResponse findById(Long id) {
		return DtoMapper.toResponse(getPost(id));
	}

	@Transactional
	public PostResponse create(PostRequest request) {
		if (postRepository.existsByPlatformAndExternalId(request.platform(), request.externalId())) {
			throw new DuplicateResourceException(
					"Post already exists: " + request.platform() + "/" + request.externalId());
		}
		Post post = new Post();
		DtoMapper.copyToEntity(request, post);
		Post saved = postRepository.save(post);
		log.info("Created post id={} platform={}", saved.getId(), saved.getPlatform());
		return DtoMapper.toResponse(saved);
	}

	@Transactional
	public PostResponse update(Long id, PostRequest request) {
		Post post = getPost(id);
		boolean keyChanged = post.getPlatform() != request.platform()
				|| !post.getExternalId().equals(request.externalId());
		if (keyChanged && postRepository.existsByPlatformAndExternalId(request.platform(), request.externalId())) {
			throw new DuplicateResourceException(
					"Post already exists: " + request.platform() + "/" + request.externalId());
		}
		DtoMapper.copyToEntity(request, post);
		return DtoMapper.toResponse(post);
	}

	@Transactional
	public void delete(Long id) {
		Post post = getPost(id);
		metricRepository.deleteByPostId(id);
		postRepository.delete(post);
		log.info("Deleted post id={}", id);
	}

	Post getPost(Long id) {
		return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
	}

}
