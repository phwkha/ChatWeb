package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry; // Thêm class này
import org.springframework.stereotype.Component;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-CHAT-CONSUMER")
public class ChatConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final SimpUserRegistry simpUserRegistry;

    private final MessageMapper messageMapper;

    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}", groupId = "chat-websocket-group-${random.uuid}")
    public void listenChatMessages(ChatMessage message) {
        if (message == null) {
            return;
        }
        String recipient = message.getRecipient();
        String sender = message.getSender();
        log.info("Kafka received message: {} -> {}", sender, recipient);
        try {
            ChatMessageResponse messageResponse = messageMapper.toResponse(message);
            messageResponse.setLocalId(message.getLocalId());
            SocketResponse<ChatMessageResponse> response = SocketResponse.message(messageResponse);
            if (recipient != null && simpUserRegistry.getUser(recipient) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        recipient,
                        "/queue/messages",
                        response);
                log.debug("Sent message via WS to recipient: {}", recipient);
            }

            if (sender != null && simpUserRegistry.getUser(sender) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        sender,
                        "/queue/messages",
                        response);
                log.debug("Synced message via WS to sender: {}", sender);
            }

            log.info("Sent message via WS to recipient: {}", message.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.chat.system-messages}", groupId = "system-websocket-group-${random.uuid}")
    public void listenSystemMessages(MessageSystemResponse systemMessage) {
        if (systemMessage == null)
            return;

        log.info("Kafka received SYSTEM message from: {}", systemMessage.getSender());

        try {
            simpMessagingTemplate.convertAndSend("/topic/public", systemMessage);
            log.info("Kafka sent SYSTEM message from: {}", systemMessage.getSender());
        } catch (Exception e) {
            log.error("Failed to send System WebSocket message: {}", e.getMessage());
        }
    }
}