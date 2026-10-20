package com.example.socialanalytics.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.JobRunRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.PostRepository.IdAndPlatform;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UpdateSocialMetricsJob {

	public static final String JOB_NAME = "updateSocialMetricsJob";

	private final AtomicBoolean running = new AtomicBoolean();

	private final PostRepository postRepository;

	private final JobRunRepository jobRunRepository;

	private final MetricsUpdateWorker worker;

	private final TaskScheduler taskScheduler;

	private final JobProperties properties;

	public UpdateSocialMetricsJob(PostRepository postRepository, JobRunRepository jobRunRepository,
			MetricsUpdateWorker worker, TaskScheduler taskScheduler, JobProperties properties) {
		this.postRepository = postRepository;
		this.jobRunRepository = jobRunRepository;
		this.worker = worker;
		this.taskScheduler = taskScheduler;
		this.properties = properties;
	}

	@Scheduled(cron = "${app.jobs.update-metrics.cron}")
	public void updateSocialMetricsJob() {
		run(JobRun.Trigger.SCHEDULED);
	}

	public boolean triggerNow() {
		if (running.get()) {
			return false;
		}
		taskScheduler.schedule(() -> run(JobRun.Trigger.MANUAL), Instant.now());
		return true;
	}

	public boolean isRunning() {
		return running.get();
	}

	public Optional<JobRun> run(JobRun.Trigger trigger) {
		if (!running.compareAndSet(false, true)) {
			log.info("{} ({}) skipped: previous run still in progress", JOB_NAME, trigger);
			return Optional.empty();
		}
		JobRun run = null;
		try {
			run = start(trigger);
			return Optional.of(execute(run));
		}
		catch (RuntimeException ex) {
			if (run == null) {
				log.error("{} ({}) could not start", JOB_NAME, trigger, ex);
				throw ex;
			}
			log.error("[job run {}] {} failed", run.getId(), JOB_NAME, ex);
			try {
				return Optional.of(finish(run, JobRun.Status.FAILED, truncate(ex.toString())));
			}
			catch (RuntimeException finishEx) {
				ex.addSuppressed(finishEx);
				throw ex;
			}
		}
		finally {
			running.set(false);
		}
	}

	private JobRun execute(JobRun run) {
		Map<SocialPlatform, List<Long>> postIdsByPlatform = postRepository.findAllIdsAndPlatforms()
			.stream()
			.collect(Collectors.groupingBy(IdAndPlatform::getPlatform,
					Collectors.mapping(IdAndPlatform::getId, Collectors.toList())));
		Set<SocialPlatform> supported = worker.supportedPlatforms();
		postIdsByPlatform.forEach((platform, ids) -> {
			if (!supported.contains(platform)) {
				log.warn("[job run {}] no API client for {}: {} posts skipped", run.getId(), platform, ids.size());
			}
		});
		postIdsByPlatform.keySet().retainAll(supported);
		run.setTotalItems(postIdsByPlatform.values().stream().mapToInt(List::size).sum());
		log.info("[job run {}] {} started ({}): {} posts {}", run.getId(), JOB_NAME, run.getTrigger(),
				run.getTotalItems(), postIdsByPlatform.entrySet()
					.stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue().size())
					.toList());

		List<CompletableFuture<ChunkResult>> futures = new ArrayList<>();
		postIdsByPlatform.forEach((platform, ids) -> {
			for (int from = 0; from < ids.size(); from += properties.chunkSize()) {
				List<Long> chunk = ids.subList(from, Math.min(from + properties.chunkSize(), ids.size()));
				futures.add(worker.updateChunk(run.getId(), platform, List.copyOf(chunk)));
			}
		});
		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		List<ChunkResult> results = futures.stream().map(CompletableFuture::join).toList();
		run.setUpdatedItems(results.stream().mapToInt(ChunkResult::updated).sum());
		run.setFailedItems(results.stream().mapToInt(ChunkResult::failed).sum());
		JobRun.Status status = (run.getFailedItems() == 0) ? JobRun.Status.SUCCESS
				: (run.getUpdatedItems() == 0) ? JobRun.Status.FAILED : JobRun.Status.PARTIAL;
		String message = results.stream().flatMap(result -> result.errors().stream()).findFirst()
			.map(first -> "First error: " + first)
			.orElse(null);
		JobRun finished = finish(run, status, truncate(message));
		log.info("[job run {}] {} finished in {}ms: {} ({} updated, {} failed, {} tasks)", run.getId(), JOB_NAME,
				finished.getDurationMs(), status, run.getUpdatedItems(), run.getFailedItems(), futures.size());
		return finished;
	}

	private JobRun start(JobRun.Trigger trigger) {
		JobRun run = new JobRun();
		run.setJobName(JOB_NAME);
		run.setTrigger(trigger);
		run.setStatus(JobRun.Status.RUNNING);
		run.setStartedAt(Instant.now());
		return jobRunRepository.save(run);
	}

	private JobRun finish(JobRun run, JobRun.Status status, String message) {
		run.setStatus(status);
		run.setFinishedAt(Instant.now());
		run.setMessage(message);
		return jobRunRepository.save(run);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void failInterruptedRuns() {
		int count = jobRunRepository.failInterruptedRuns(Instant.now());
		if (count > 0) {
			log.warn("Marked {} interrupted {} run(s) as FAILED", count, JOB_NAME);
		}
	}

	private static String truncate(String value) {
		return (value == null || value.length() <= 500) ? value : value.substring(0, 497) + "...";
	}

}
