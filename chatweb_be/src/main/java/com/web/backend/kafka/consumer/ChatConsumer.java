package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.event.ChatMessageEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-CHAT-CONSUMER")
public class ChatConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @KafkaListener(topics = "${spring.kafka.topic.chat.new-message}")
    public void listenChatMessages(ChatMessageEvent message) {
        log.info("Received message from Kafka: from {} to {}", message.getSenderUsername(),
                message.getRecipientUsername());

        try {

            SocketResponse<ChatMessageResponse> response = SocketResponse.message(message.getChatMessageResponse());

            simpMessagingTemplate.convertAndSendToUser(
                    message.getRecipientUsername(),
                    "/queue/messages",
                    response);

            simpMessagingTemplate.convertAndSendToUser(
                    message.getSenderUsername(),
                    "/queue/messages",
                    response);

        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
        }
    }
}