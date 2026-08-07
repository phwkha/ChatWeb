package com.web.backend.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.web.backend.exception.WebSocketErrorHandler;

@Component
@Slf4j(topic = "CHAT-KAFKA-PRODUCER")
@RequiredArgsConstructor
public class ChatProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.chat.messages}")
    private String chatTopic;

    @Value("${spring.kafka.topic.chat.system-messages}")
    private String systemTopic;

    private final WebSocketErrorHandler webSocketErrorHandler;

    public CompletableFuture<SendResult<String, Object>> sendChatMessage(Object messageChat, String sender, Object request) {
        return sendSafely(chatTopic, messageChat, "Chat Message", sender, request);
    }

    public CompletableFuture<SendResult<String, Object>> sendSystemMessage(Object messageSystem, String sender, Object request) {
        return sendSafely(systemTopic, messageSystem, "System Message", sender, request);
    }

    public CompletableFuture<SendResult<String, Object>> sendReaction(Object messageReaction, String sender, Object request) {
        return sendSafely(chatTopic, messageReaction, "Reaction", sender, request);
    }

    public CompletableFuture<SendResult<String, Object>> sendEditMessage(Object messageEdit, String sender, Object request) {
        return sendSafely(chatTopic, messageEdit, "Edit Message", sender, request);
    }

    public CompletableFuture<SendResult<String, Object>> sendRevokeMessage(Object messageRevoke, String sender, Object request) {
        return sendSafely(chatTopic, messageRevoke, "Revoke Message", sender, request);
    }

    public CompletableFuture<SendResult<String, Object>> sendStatusMessage(Object statusMsg, String sender, Object request) {
        return sendSafely(chatTopic, statusMsg, "Status Message", sender, request);
    }

    private CompletableFuture<SendResult<String, Object>> sendSafely(String topic, Object payload, String actionName, String sender, Object request) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(Objects.requireNonNull(topic),
                payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Critical Error: Cannot push {} to Kafka. Topic: {}", actionName, topic, ex);
                webSocketErrorHandler.handleChatError(sender, request, com.web.backend.config.localresolverconfig.Translator.tolocale("error.msg.system_overload"));
            } else {
                log.debug("{}: Kafka push successful offset: {}", actionName, result.getRecordMetadata().offset());
            }
        });
        return future;
    }
}
