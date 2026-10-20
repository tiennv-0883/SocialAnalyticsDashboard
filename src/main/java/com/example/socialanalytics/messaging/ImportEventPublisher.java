package com.example.socialanalytics.messaging;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportEventPublisher {

	private final JmsTemplate jmsTemplate;

	public void publish(ImportCompletedEvent event) {
		try {
			jmsTemplate.convertAndSend(Queues.IMPORT_COMPLETED, event);
			log.info("Published {} importId={} ({} imported)", ImportCompletedEvent.TYPE, event.importId(),
					event.imported());
		}
		catch (JmsException ex) {
			log.error("Could not publish {} importId={}", ImportCompletedEvent.TYPE, event.importId(), ex);
		}
	}

}
