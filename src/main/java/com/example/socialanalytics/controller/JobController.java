package com.example.socialanalytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socialanalytics.dto.JobRunResponse;
import com.example.socialanalytics.job.UpdateSocialMetricsJob;
import com.example.socialanalytics.repository.JobRunRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Jobs", description = "Background job that refreshes post metrics")
@RestController
@RequestMapping("/jobs/update-metrics")
@RequiredArgsConstructor
public class JobController {

	private final UpdateSocialMetricsJob job;

	private final JobRunRepository jobRunRepository;

	@Operation(summary = "Start updateSocialMetricsJob now (runs in the background)")
	@PostMapping("/run")
	public ResponseEntity<?> run() {
		if (!job.triggerNow()) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Job is already running"));
		}
		return ResponseEntity.accepted().build();
	}

	@Operation(summary = "Latest run of updateSocialMetricsJob (204 if it never ran)")
	@GetMapping("/last")
	public ResponseEntity<JobRunResponse> last() {
		return jobRunRepository.findTopByJobNameOrderByStartedAtDesc(UpdateSocialMetricsJob.JOB_NAME)
			.map(JobRunResponse::of)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

}
