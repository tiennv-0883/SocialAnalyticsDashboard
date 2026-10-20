package com.example.socialanalytics.realtime;

import java.time.Instant;

import com.example.socialanalytics.entity.JobRun;

public record MetricsUpdatedEvent(Long runId, JobRun.Trigger trigger, JobRun.Status status, int updated, int failed,
		Instant finishedAt) {

	public static MetricsUpdatedEvent of(JobRun run) {
		return new MetricsUpdatedEvent(run.getId(), run.getTrigger(), run.getStatus(), run.getUpdatedItems(),
				run.getFailedItems(), run.getFinishedAt());
	}

}
