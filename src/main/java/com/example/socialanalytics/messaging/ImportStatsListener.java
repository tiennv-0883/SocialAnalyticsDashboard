package com.example.socialanalytics.messaging;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.socialanalytics.entity.ImportLog;
import com.example.socialanalytics.entity.PlatformStats;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.ImportLogRepository;
import com.example.socialanalytics.repository.PlatformStatsRepository;
import com.example.socialanalytics.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportStatsListener {

	private final ImportLogRepository importLogRepository;

	private final PlatformStatsRepository statsRepository;

	private final PostRepository postRepository;

	@JmsListener(destination = Queues.IMPORT_COMPLETED)
	@Transactional
	public void onImportCompleted(ImportCompletedEvent event) {
		validate(event);
		if (importLogRepository.existsByImportId(event.importId())) {
			log.info("{} importId={} already processed, skipping (redelivery)", ImportCompletedEvent.TYPE,
					event.importId());
			return;
		}
		ImportLog importLog = new ImportLog();
		importLog.setImportId(event.importId());
		importLog.setFileName(event.fileName());
		importLog.setTotalRows(event.totalRows());
		importLog.setImported(event.imported());
		importLog.setFailed(event.failed());
		importLog.setImportedAt(event.occurredAt());
		importLogRepository.save(importLog);

		Map<SocialPlatform, Integer> importedByPlatform = (event.importedByPlatform() == null) ? Map.of()
				: event.importedByPlatform();
		Map<SocialPlatform, Long> postCounts = new EnumMap<>(SocialPlatform.class);
		postRepository.countByPlatform().forEach(count -> postCounts.put(count.getPlatform(), count.getTotal()));
		for (SocialPlatform platform : SocialPlatform.values()) {
			PlatformStats stats = statsRepository.findByPlatform(platform).orElseGet(() -> {
				PlatformStats created = new PlatformStats();
				created.setPlatform(platform);
				return created;
			});
			stats.setPostCount(postCounts.getOrDefault(platform, 0L));
			int imported = importedByPlatform.getOrDefault(platform, 0);
			if (imported > 0) {
				stats.setImportedPosts(stats.getImportedPosts() + imported);
				stats.setImportCount(stats.getImportCount() + 1);
				stats.setLastImportAt(event.occurredAt());
			}
			statsRepository.save(stats);
		}
		log.info("Processed {} importId={}: stats updated {}", ImportCompletedEvent.TYPE, event.importId(),
				postCounts);
	}

	private static void validate(ImportCompletedEvent event) {
		if (event == null || !StringUtils.hasText(event.importId()) || event.occurredAt() == null
				|| event.imported() < 0 || event.failed() < 0) {
			throw new IllegalArgumentException("Invalid " + ImportCompletedEvent.TYPE + " message: " + event);
		}
	}

}
