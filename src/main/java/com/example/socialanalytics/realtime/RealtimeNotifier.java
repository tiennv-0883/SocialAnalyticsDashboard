package com.example.socialanalytics.realtime;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeNotifier {

	public static final String TOPIC = "/topic/metrics";

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener(fallbackExecution = true)
	public void onMetricsUpdated(MetricsUpdatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runId", event.runId());
		data.put("trigger", event.trigger());
		data.put("status", event.status());
		data.put("updated", event.updated());
		data.put("failed", event.failed());
		send(new LiveNotification(LiveNotification.METRICS_UPDATED, "Đã cập nhật metrics: " + event.updated()
				+ " bài" + (event.failed() > 0 ? ", lỗi " + event.failed() + " bài" : "") + " (" + event.status() + ")",
				event.finishedAt(), data));
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onImportStatsUpdated(ImportStatsUpdatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("importId", event.importId());
		data.put("fileName", event.fileName());
		data.put("imported", event.imported());
		data.put("failed", event.failed());
		send(new LiveNotification(LiveNotification.IMPORT_COMPLETED, "Import xong '" + event.fileName() + "': "
				+ event.imported() + " bài mới" + (event.failed() > 0 ? ", lỗi " + event.failed() + " dòng" : ""),
				event.occurredAt(), data));
	}

	private void send(LiveNotification notification) {
		try {
			messagingTemplate.convertAndSend(TOPIC, notification);
			log.debug("Broadcast {} to {}", notification.type(), TOPIC);
		}
		catch (MessagingException ex) {
			log.warn("Could not broadcast {}: {}", notification.type(), ex.getMessage());
		}
	}

}
