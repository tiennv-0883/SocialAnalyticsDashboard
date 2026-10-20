package com.example.socialanalytics.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_runs", indexes = @Index(columnList = "job_name, started_at"))
public class JobRun extends BaseEntity {

	public enum Trigger {
		SCHEDULED, MANUAL
	}

	public enum Status {
		RUNNING, SUCCESS, PARTIAL, FAILED
	}

	@Column(name = "job_name", nullable = false, length = 100)
	private String jobName;

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false, length = 20)
	private Trigger trigger;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "total_items", nullable = false)
	private int totalItems;

	@Column(name = "updated_items", nullable = false)
	private int updatedItems;

	@Column(name = "failed_items", nullable = false)
	private int failedItems;

	@Column(length = 500)
	private String message;

	public Long getDurationMs() {
		return (finishedAt == null) ? null : finishedAt.toEpochMilli() - startedAt.toEpochMilli();
	}

}
