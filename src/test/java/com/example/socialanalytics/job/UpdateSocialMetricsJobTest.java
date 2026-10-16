package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.JobRunRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.PostRepository.IdAndPlatform;

class UpdateSocialMetricsJobTest {

	private final PostRepository postRepository = mock(PostRepository.class);

	private final JobRunRepository jobRunRepository = mock(JobRunRepository.class);

	private final MetricsUpdateWorker worker = mock(MetricsUpdateWorker.class);

	private final UpdateSocialMetricsJob job = new UpdateSocialMetricsJob(postRepository, jobRunRepository, worker,
			mock(TaskScheduler.class), new JobProperties("-", 2, 1, 1, 1));

	@BeforeEach
	void saveAssignsId() {
		when(jobRunRepository.save(any(JobRun.class))).thenAnswer(invocation -> {
			JobRun run = invocation.getArgument(0);
			if (run.getId() == null) {
				run.setId(1L);
			}
			return run;
		});
	}

	@Test
	void splitsPostsPerPlatformIntoChunksAndSucceeds() {
		givenPosts(post(1, SocialPlatform.FACEBOOK), post(2, SocialPlatform.FACEBOOK), post(3, SocialPlatform.FACEBOOK),
				post(4, SocialPlatform.TWITTER));
		when(worker.updateChunk(anyLong(), any(), any()))
			.thenAnswer(invocation -> done(((List<?>) invocation.getArgument(2)).size(), List.of()));

		JobRun run = job.run(JobRun.Trigger.MANUAL).orElseThrow();

		assertThat(run.getStatus()).isEqualTo(JobRun.Status.SUCCESS);
		assertThat(run.getTotalItems()).isEqualTo(4);
		assertThat(run.getUpdatedItems()).isEqualTo(4);
		assertThat(run.getFinishedAt()).isNotNull();
		verify(worker).updateChunk(1L, SocialPlatform.FACEBOOK, List.of(1L, 2L));
		verify(worker).updateChunk(1L, SocialPlatform.FACEBOOK, List.of(3L));
		verify(worker).updateChunk(1L, SocialPlatform.TWITTER, List.of(4L));
	}

	@Test
	void someFailuresMakeRunPartial() {
		givenPosts(post(1, SocialPlatform.FACEBOOK), post(2, SocialPlatform.FACEBOOK));
		when(worker.updateChunk(anyLong(), any(), any())).thenReturn(done(1, List.of("post 2: timeout")));

		JobRun run = job.run(JobRun.Trigger.SCHEDULED).orElseThrow();

		assertThat(run.getStatus()).isEqualTo(JobRun.Status.PARTIAL);
		assertThat(run.getFailedItems()).isEqualTo(1);
		assertThat(run.getMessage()).isEqualTo("First error: post 2: timeout");
	}

	@Test
	void unexpectedErrorMarksRunFailedAndReleasesLock() {
		when(postRepository.findAllIdsAndPlatforms()).thenThrow(new IllegalStateException("db down"));

		JobRun run = job.run(JobRun.Trigger.SCHEDULED).orElseThrow();

		assertThat(run.getStatus()).isEqualTo(JobRun.Status.FAILED);
		assertThat(run.getMessage()).contains("db down");
		assertThat(job.isRunning()).isFalse();
	}

	@Test
	void runStartedWhileAnotherIsActiveIsSkipped() throws Exception {
		givenPosts(post(1, SocialPlatform.FACEBOOK));
		CountDownLatch workerStarted = new CountDownLatch(1);
		CompletableFuture<ChunkResult> pending = new CompletableFuture<>();
		when(worker.updateChunk(anyLong(), eq(SocialPlatform.FACEBOOK), any())).thenAnswer(invocation -> {
			workerStarted.countDown();
			return pending;
		});
		Thread first = new Thread(() -> job.run(JobRun.Trigger.SCHEDULED));
		first.start();
		assertThat(workerStarted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(job.isRunning()).isTrue();
		assertThat(job.run(JobRun.Trigger.MANUAL)).isEmpty();
		assertThat(job.triggerNow()).isFalse();

		pending.complete(new ChunkResult(1, List.of()));
		first.join(5_000);
		assertThat(job.isRunning()).isFalse();
		verify(worker, times(1)).updateChunk(anyLong(), any(), any());
	}

	private void givenPosts(IdAndPlatform... posts) {
		when(postRepository.findAllIdsAndPlatforms()).thenReturn(List.of(posts));
	}

	private static CompletableFuture<ChunkResult> done(int updated, List<String> errors) {
		return CompletableFuture.completedFuture(new ChunkResult(updated, errors));
	}

	private static IdAndPlatform post(long id, SocialPlatform platform) {
		return new IdAndPlatform() {

			@Override
			public Long getId() {
				return id;
			}

			@Override
			public SocialPlatform getPlatform() {
				return platform;
			}

		};
	}

}
