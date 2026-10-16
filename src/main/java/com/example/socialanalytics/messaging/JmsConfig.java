package com.example.socialanalytics.messaging;

import java.util.Map;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.artemis.autoconfigure.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {

	@Bean
	public MessageConverter jacksonJmsMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_type");
		converter.setTypeIdMappings(Map.of(ImportCompletedEvent.TYPE, ImportCompletedEvent.class));
		return converter;
	}

	@Bean
	public ArtemisConfigurationCustomizer importQueueRetryPolicy(
			@Value("${app.jms.import-completed.max-delivery-attempts:3}") int maxDeliveryAttempts,
			@Value("${app.jms.import-completed.redelivery-delay-ms:1000}") long redeliveryDelayMs) {
		return configuration -> configuration.addAddressSetting(Queues.IMPORT_COMPLETED,
				new AddressSettings().setMaxDeliveryAttempts(maxDeliveryAttempts)
					.setRedeliveryDelay(redeliveryDelayMs)
					.setRedeliveryMultiplier(2.0)
					.setMaxRedeliveryDelay(redeliveryDelayMs * 10)
					.setDeadLetterAddress(SimpleString.of(Queues.IMPORT_COMPLETED_DLQ)));
	}

}
