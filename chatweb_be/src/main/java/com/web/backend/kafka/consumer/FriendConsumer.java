package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import com.web.backend.kafka.payload.FriendNotificationMessage;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-FRIEND-CONSUMER")
public class FriendConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final SimpUserRegistry simpUserRegistry;

    private static final String DESTINATION_MUST_NOT_BE_NULL_STRING = "Destination must not be null";

    private static final String RESPONSE_MUST_NOT_BE_NULL_STRING = "Response must not be null";

    @KafkaListener(topics = "friend-notifications", groupId = "friend-websocket-group-${random.uuid}")
    public void listenFriendNotifications(FriendNotificationMessage payload) {
        if (payload == null) {
            return;
        }

        String recipient = payload.getRecipientUsername();
        String sender = payload.getSenderUsername();

        try {
            if (recipient != null && payload.getRecipientResponse() != null
                    && simpUserRegistry.getUser(recipient) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        recipient,
                        Objects.requireNonNull(payload.getDestination(), DESTINATION_MUST_NOT_BE_NULL_STRING),
                        Objects.requireNonNull(payload.getRecipientResponse(), RESPONSE_MUST_NOT_BE_NULL_STRING));
                log.info("Sent friend notification via WS to recipient: {}", recipient);
            }
            if (sender != null && payload.getSenderResponse() != null && simpUserRegistry.getUser(sender) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        sender,
                        Objects.requireNonNull(payload.getDestination(), DESTINATION_MUST_NOT_BE_NULL_STRING),
                        Objects.requireNonNull(payload.getSenderResponse(), RESPONSE_MUST_NOT_BE_NULL_STRING));
                log.info("Sent friend notification via WS to sender: {}", sender);
            }
        } catch (Exception e) {
            log.error("Error sending WS notification: {}", e.getMessage());
        }
    }
}
