package com.example.socialanalytics.realtime;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;

public class InboundMessageGuard implements ChannelInterceptor {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		SimpMessageType type = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		if (type == SimpMessageType.MESSAGE) {
			throw new MessageDeliveryException(message, "Clients are not allowed to send messages");
		}
		if (type == SimpMessageType.SUBSCRIBE && !RealtimeNotifier.TOPIC.equals(destination)) {
			throw new MessageDeliveryException(message, "Subscription not allowed: " + destination);
		}
		return message;
	}

}
