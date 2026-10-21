package com.example.socialanalytics.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.job.UpdateSocialMetricsJob;
import com.example.socialanalytics.messaging.ImportCompletedEvent;
import com.example.socialanalytics.messaging.Queues;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RealtimeBroadcastTests {

	@Autowired
	@Qualifier("brokerChannel")
	private AbstractSubscribableChannel brokerChannel;

	@Autowired
	private UpdateSocialMetricsJob job;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private MockMvc mockMvc;

	private final List<String> broadcasts = new CopyOnWriteArrayList<>();

	private final ChannelInterceptor capture = new ChannelInterceptor() {

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			if (RealtimeNotifier.TOPIC.equals(SimpMessageHeaderAccessor.getDestination(message.getHeaders()))) {
				broadcasts.add(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8));
			}
			return message;
		}

	};

	@BeforeEach
	void listen() {
		brokerChannel.addInterceptor(capture);
	}

	@AfterEach
	void stopListening() {
		brokerChannel.removeInterceptor(capture);
	}

	@Test
	void finishedJobRunIsBroadcast() {
		JobRun run = job.run(JobRun.Trigger.MANUAL).orElseThrow();

		assertThat(broadcasts).anySatisfy(json -> assertThat(json).contains("\"type\":\"METRICS_UPDATED\"")
			.contains("\"runId\":" + run.getId()));
	}

	@Test
	void processedImportMessageIsBroadcastAfterCommit() {
		String importId = UUID.randomUUID().toString();
		jmsTemplate.convertAndSend(Queues.IMPORT_COMPLETED, new ImportCompletedEvent(importId, "live.xlsx", 2, 2, 0,
				Map.of(SocialPlatform.FACEBOOK, 2), Instant.now()));

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(broadcasts)
			.anySatisfy(json -> assertThat(json).contains("\"type\":\"IMPORT_COMPLETED\"").contains(importId)));
	}

	@Test
	void chartDataRequiresLoginAndValidatesDays() throws Exception {
		mockMvc.perform(get("/chart-data")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/ws/info")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/chart-data").param("days", "0").with(oauth2Login())).andExpect(status().isBadRequest());
		mockMvc.perform(get("/chart-data").param("days", "7").with(oauth2Login()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.labels.length()").value(7))
			.andExpect(jsonPath("$.series.likes.length()").value(7));
	}

	@Test
	void dashboardIncludesChartsAndRealtimeClient() throws Exception {
		mockMvc.perform(get("/dashboard").with(oauth2Login()))
			.andExpect(status().isOk())
			.andExpect(content()
				.string(allOf(containsString("id=\"engagementChart\""),
						containsString("/js/dashboard-chart.js"),
						containsString("sockjs.min.js"))));
	}

}
