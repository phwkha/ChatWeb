package com.web.backend.kafka.producer;

import com.web.backend.event.ChatMessageEvent;
import com.web.backend.kafka.payload.ChatMessagePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-EVENT-PRODUCER")
public class ChatProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.chat.new-message}")
    private String TOPIC_NEW_MESSAGE;

    @Async
    @EventListener
    public void handleNewChatMessage(ChatMessageEvent event) {
        log.info("Publishing new message to Kafka Topic '{}' from {} to {}",
                TOPIC_NEW_MESSAGE, event.getSenderUsername(), event.getRecipientUsername());
        ChatMessagePayload payload = new ChatMessagePayload(
                event.getResponse(),
                event.getSenderUsername(),
                event.getRecipientUsername());
        kafkaTemplate.send(Objects.requireNonNull(TOPIC_NEW_MESSAGE), payload);
    }
}