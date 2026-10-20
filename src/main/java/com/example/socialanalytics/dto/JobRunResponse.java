package com.example.socialanalytics.dto;

import java.time.Instant;

import com.example.socialanalytics.entity.JobRun;

public record JobRunResponse(Long id, String jobName, JobRun.Trigger trigger, JobRun.Status status, Instant startedAt,
		Instant finishedAt, Long durationMs, int totalItems, int updatedItems, int failedItems, String message) {

	public static JobRunResponse of(JobRun run) {
		return new JobRunResponse(run.getId(), run.getJobName(), run.getTrigger(), run.getStatus(), run.getStartedAt(),
				run.getFinishedAt(), run.getDurationMs(), run.getTotalItems(), run.getUpdatedItems(),
				run.getFailedItems(), run.getMessage());
	}

}
