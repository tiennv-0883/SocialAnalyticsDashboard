package com.example.socialanalytics.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.example.socialanalytics.entity.PlatformStats;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelTestFiles;
import com.example.socialanalytics.repository.ImportLogRepository;
import com.example.socialanalytics.repository.PlatformStatsRepository;
import com.example.socialanalytics.service.PostExcelService;

/**
 * Embedded Artemis broker: import publishes IMPORT_COMPLETED, the listener updates the
 * statistics; broken messages are retried and end up in the DLQ.
 */
@SpringBootTest
@ActiveProfiles("test")
class ImportMessagingTests {

	@Autowired
	private PostExcelService postExcelService;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private ImportLogRepository importLogRepository;

	@Autowired
	private PlatformStatsRepository statsRepository;

	@Test
	void importPublishesEventAndListenerUpdatesStatistics() throws Exception {
		long importedBefore = importedPosts(SocialPlatform.TWITTER);
		long logsBefore = importLogRepository.count();
		byte[] excel = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url" },
				{ "TWITTER", "jms-tw-1", "https://x.com/1" }, { "TWITTER", "jms-tw-2", "https://x.com/2" },
				{ "TWITTER", "", "missing id" } });

		postExcelService.importPosts(new MockMultipartFile("file", "jms.xlsx", null, excel));

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			assertThat(importLogRepository.count()).isEqualTo(logsBefore + 1);
			assertThat(importedPosts(SocialPlatform.TWITTER)).isEqualTo(importedBefore + 2);
		});
		PlatformStats twitter = statsRepository.findByPlatform(SocialPlatform.TWITTER).orElseThrow();
		assertThat(twitter.getPostCount()).isGreaterThanOrEqualTo(2);
		assertThat(twitter.getLastImportAt()).isNotNull();
	}

	@Test
	void sameEventDeliveredTwiceIsCountedOnce() {
		long importedBefore = importedPosts(SocialPlatform.FACEBOOK);
		ImportCompletedEvent event = new ImportCompletedEvent(UUID.randomUUID().toString(), "dup.xlsx", 1, 1, 0,
				Map.of(SocialPlatform.FACEBOOK, 1), Instant.now());

		jmsTemplate.convertAndSend(Queues.IMPORT_COMPLETED, event);
		jmsTemplate.convertAndSend(Queues.IMPORT_COMPLETED, event);

		await().atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> assertThat(importLogRepository.existsByImportId(event.importId())).isTrue());
		await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5))
			.untilAsserted(() -> assertThat(importedPosts(SocialPlatform.FACEBOOK)).isEqualTo(importedBefore + 1));
	}

	@Test
	void invalidMessageIsRetriedThenMovedToDeadLetterQueue() {
		int dlqBefore = dlqSize();
		ImportCompletedEvent poison = new ImportCompletedEvent("", "broken.xlsx", 1, 1, 0, Map.of(), Instant.now());

		jmsTemplate.convertAndSend(Queues.IMPORT_COMPLETED, poison);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(dlqSize()).isEqualTo(dlqBefore + 1));
	}

	private long importedPosts(SocialPlatform platform) {
		return statsRepository.findByPlatform(platform).map(PlatformStats::getImportedPosts).orElse(0L);
	}

	private int dlqSize() {
		Integer size = jmsTemplate.browse(Queues.IMPORT_COMPLETED_DLQ,
				(session, browser) -> Collections.list(browser.getEnumeration()).size());
		return size == null ? 0 : size;
	}

}
