package com.example.socialanalytics.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.dto.PostResponse;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Posts", description = "Manage tracked social posts")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@Operation(summary = "List posts, optionally filtered by platform")
	@GetMapping
	public Page<PostResponse> list(@RequestParam(required = false) SocialPlatform platform,
			@PageableDefault(size = 20) Pageable pageable) {
		return postService.findAll(platform, pageable);
	}

	@Operation(summary = "Get a post by id")
	@GetMapping("/{id}")
	public PostResponse get(@PathVariable Long id) {
		return postService.findById(id);
	}

	@Operation(summary = "Create a post")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(@Valid @RequestBody PostRequest request) {
		return postService.create(request);
	}

	@Operation(summary = "Update a post")
	@PutMapping("/{id}")
	public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
		return postService.update(id, request);
	}

	@Operation(summary = "Delete a post and its metrics")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		postService.delete(id);
	}

}
