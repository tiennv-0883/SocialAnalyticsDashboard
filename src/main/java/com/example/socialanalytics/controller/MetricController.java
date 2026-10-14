package com.example.socialanalytics.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.example.socialanalytics.dto.MetricRequest;
import com.example.socialanalytics.dto.MetricResponse;
import com.example.socialanalytics.service.MetricService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Metrics", description = "Manage engagement snapshots of posts")
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

	private final MetricService metricService;

	@Operation(summary = "List metrics, optionally filtered by post")
	@GetMapping
	public Page<MetricResponse> list(@RequestParam(required = false) Long postId,
			@PageableDefault(size = 20, sort = "collectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return metricService.findAll(postId, pageable);
	}

	@Operation(summary = "Get a metric by id")
	@GetMapping("/{id}")
	public MetricResponse get(@PathVariable Long id) {
		return metricService.findById(id);
	}

	@Operation(summary = "Record a metric snapshot for a post")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MetricResponse create(@Valid @RequestBody MetricRequest request) {
		return metricService.create(request);
	}

	@Operation(summary = "Update a metric snapshot")
	@PutMapping("/{id}")
	public MetricResponse update(@PathVariable Long id, @Valid @RequestBody MetricRequest request) {
		return metricService.update(id, request);
	}

	@Operation(summary = "Delete a metric snapshot")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		metricService.delete(id);
	}

}
