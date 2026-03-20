package com.web.backend.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.KafkaChatMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-CHAT-LISTENER")
public class KafkaChatListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @KafkaListener(topics = "${spring.kafka.topic}")
    public void listenChatMessages(KafkaChatMessageResponse message) {
        log.info("Received message from Kafka: {}", message.getRecipientUsername());

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