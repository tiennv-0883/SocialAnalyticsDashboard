package com.example.socialanalytics.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class InboundMessageGuardTest {

	private final InboundMessageGuard guard = new InboundMessageGuard();

	@Test
	void allowsConnectAndSubscribeToMetricsTopic() {
		assertThat(guard.preSend(frame(StompCommand.CONNECT, null), null)).isNotNull();
		assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, RealtimeNotifier.TOPIC), null)).isNotNull();
	}

	@Test
	void rejectsClientSendSoNobodyCanSpoofBroadcasts() {
		assertThatThrownBy(() -> guard.preSend(frame(StompCommand.SEND, RealtimeNotifier.TOPIC), null))
			.isInstanceOf(MessageDeliveryException.class);
	}

	@Test
	void rejectsSubscriptionToOtherDestinations() {
		assertThatThrownBy(() -> guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/other"), null))
			.isInstanceOf(MessageDeliveryException.class);
	}

	private static Message<byte[]> frame(StompCommand command, String destination) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		if (destination != null) {
			accessor.setDestination(destination);
		}
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

}
