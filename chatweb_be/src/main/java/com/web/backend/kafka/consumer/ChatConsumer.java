package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.backend.redis.RedisWsMessage;
import com.web.backend.config.ServerIdentity;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-KAFKA-CONSUMER")
public class ChatConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final MessageMapper messageMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String QUEUE_MESSAGES_STRING = "/queue/messages";

    private static final String TOPIC_PUBLIC_STRING = "/topic/public";

    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}", groupId = "${spring.kafka.topic.chat.messages-group-id}")
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

            routeMessage(recipient, QUEUE_MESSAGES_STRING, response);
            routeMessage(sender, QUEUE_MESSAGES_STRING, response);

            log.info("Finished processing Kafka message for recipient: {}", message.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
            if (message != null && message.getSender() != null) {
                routeMessage(message.getSender(), "/queue/errors", SocketResponse.error("System error while processing your message", null));
            }
        }
    }

    private void routeMessage(String username, String destination, Object payload) {
        if (username == null)
            return;
        try {
            String targetServerId = (String) redisTemplate.opsForValue().get("ws:routing:" + username);
            if (targetServerId != null) {
                if (ServerIdentity.SERVER_ID.equals(targetServerId)) {
                    simpMessagingTemplate.convertAndSendToUser(username, destination, payload);
                    log.info("Sent locally to {}", username);
                } else {
                    RedisWsMessage wsMessage = new RedisWsMessage(username, destination, payload);
                    redisTemplate.convertAndSend("channel:server:" + targetServerId,
                            objectMapper.writeValueAsString(wsMessage));
                    log.info("Routed to Server {} for user {}", targetServerId, username);
                }
            } else {
                log.info("User {} is offline, skipped routing.", username);
            }
        } catch (Exception e) {
            log.error("Error routing message for {}: {}", username, e.getMessage());
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.chat.system-messages}", groupId = "${spring.kafka.topic.chat.system-messages-group-id}-${random.uuid}")
    public void listenSystemMessages(SystemMessage systemMessage) {
        if (systemMessage == null)
            return;

        MessageSystemResponse response = messageMapper.systemMessageToResponse(systemMessage);

        log.info("Kafka received SYSTEM message from: {}", response.getSender());

        try {
            simpMessagingTemplate.convertAndSend(TOPIC_PUBLIC_STRING, response);
            log.info("Kafka sent SYSTEM message from: {}", response.getSender());
        } catch (Exception e) {
            log.error("Failed to send System WebSocket message: {}", e.getMessage());
        }
    }
}