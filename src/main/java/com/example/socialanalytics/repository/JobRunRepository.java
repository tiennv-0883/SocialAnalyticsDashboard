package com.example.socialanalytics.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.entity.JobRun;

public interface JobRunRepository extends JpaRepository<JobRun, Long> {

	Optional<JobRun> findTopByJobNameOrderByStartedAtDesc(String jobName);

	Optional<JobRun> findTopByJobNameAndStatusInOrderByFinishedAtDesc(String jobName, Collection<JobRun.Status> statuses);

	@Transactional
	@Modifying
	@Query("""
			update JobRun r set r.status = com.example.socialanalytics.entity.JobRun.Status.FAILED,
			r.finishedAt = :now, r.message = 'Interrupted (application stopped while running)'
			where r.status = com.example.socialanalytics.entity.JobRun.Status.RUNNING
			""")
	int failInterruptedRuns(Instant now);

}
