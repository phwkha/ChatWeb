package com.web.backend.kafka.producer;

import com.web.backend.event.KafkaChatMessageEvent;
import com.web.backend.event.NewChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-EVENT-PRODUCER")
public class KafkaChatProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.chat-topic}")
    private String TOPIC;

    @Async
    @EventListener
    public void handleNewChatMessage(NewChatMessageEvent event) {
        log.info("Publishing new message to Kafka Topic '{}' from {} to {}",
                TOPIC, event.getSenderUsername(), event.getRecipientUsername());
        KafkaChatMessageEvent payload = new KafkaChatMessageEvent(
                event.getResponse(),
                event.getSenderUsername(),
                event.getRecipientUsername());
        kafkaTemplate.send(TOPIC, payload);
    }
}