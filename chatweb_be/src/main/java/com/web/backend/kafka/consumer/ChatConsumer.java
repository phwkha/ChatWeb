package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry; // Thêm class này
import org.springframework.stereotype.Component;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.kafka.payload.ChatMessagePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-CHAT-CONSUMER")
public class ChatConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    @KafkaListener(topics = "${spring.kafka.topic.chat.new-message}", groupId = "chat-websocket-group-${random.uuid}")
    public void listenChatMessages(ChatMessagePayload message) {
        if (message == null) {
            return;
        }
        String recipient = message.getRecipientUsername();
        String sender = message.getSenderUsername();
        log.info("Kafka nhận tin nhắn: {} -> {}", sender, recipient);
        try {
            SocketResponse<ChatMessageResponse> response = SocketResponse.message(message.getChatMessageResponse());
            if (recipient != null && simpUserRegistry.getUser(recipient) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        recipient,
                        "/queue/messages",
                        response);
                log.debug("Đã gửi tin nhắn qua WS cho người nhận: {}", recipient);
            }

            if (sender != null && simpUserRegistry.getUser(sender) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        sender,
                        "/queue/messages",
                        response);
                log.debug("Đã đồng bộ tin nhắn qua WS cho người gửi: {}", sender);
            }

            log.debug("Đã gửi tin nhắn qua WS cho người nhận: {}", message.getRecipientUsername());
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
        }
    }
}