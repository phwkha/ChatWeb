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
                        Objects.requireNonNull(payload.getDestination(), "Destination must not be null"),
                        Objects.requireNonNull(payload.getRecipientResponse(), "Response must not be null"));
                log.info("Đã gửi notification kết bạn qua WS cho người nhận: {}", recipient);
            }
            if (sender != null && payload.getSenderResponse() != null && simpUserRegistry.getUser(sender) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        sender,
                        Objects.requireNonNull(payload.getDestination(), "Destination must not be null"),
                        Objects.requireNonNull(payload.getSenderResponse(), "Response must not be null"));
                log.info("Đã gửi notification kết bạn qua WS cho người gửi: {}", sender);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi WS notification: {}", e.getMessage());
        }
    }
}
