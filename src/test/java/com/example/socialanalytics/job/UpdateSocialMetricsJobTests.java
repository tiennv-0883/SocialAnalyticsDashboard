package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

/**
 * Real thread pool + database: the job updates every post on {@code metrics-*} threads.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateSocialMetricsJobTests {

	@Autowired
	private UpdateSocialMetricsJob job;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private SocialMetricRepository metricRepository;

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean(name = "facebookApiClient")
	private SocialApiClient facebookApiClient;

	@Test
	void updatesEveryPostOnWorkerThreads() {
		for (int i = 0; i < 5; i++) {
			savePost(SocialPlatform.FACEBOOK, "job-fb-" + i);
		}
		savePost(SocialPlatform.TWITTER, "job-tw-1");
		Set<String> threads = ConcurrentHashMap.newKeySet();
		doAnswer(invocation -> {
			threads.add(Thread.currentThread().getName());
			return invocation.callRealMethod();
		}).when(facebookApiClient).fetchMetrics(any(), any());
		long metricsBefore = metricRepository.count();
		long posts = postRepository.count();

		JobRun run = job.run(JobRun.Trigger.MANUAL).orElseThrow();

		assertThat(run.getStatus()).isEqualTo(JobRun.Status.SUCCESS);
		assertThat(run.getUpdatedItems()).isEqualTo(posts);
		assertThat(metricRepository.count()).isEqualTo(metricsBefore + posts);
		assertThat(threads).isNotEmpty().allMatch(name -> name.startsWith("metrics-"));
	}

	@Test
	void jobApiStartsRunInBackgroundAndReportsLastRun() throws Exception {
		mockMvc.perform(post("/jobs/update-metrics/run").with(csrf())).andExpect(status().isUnauthorized());

		mockMvc.perform(post("/jobs/update-metrics/run").with(oauth2Login()).with(csrf()))
			.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mockMvc
			.perform(get("/jobs/update-metrics/last").with(oauth2Login()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trigger").value("MANUAL"))
			.andExpect(jsonPath("$.status").value("SUCCESS")));
	}

	private void savePost(SocialPlatform platform, String externalId) {
		Post post = new Post();
		post.setPlatform(platform);
		post.setExternalId(externalId);
		post.setUrl("https://example.com/" + externalId);
		postRepository.save(post);
	}

}
