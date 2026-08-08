package com.web.backend.kafka.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DEAD-LETTER-CONSUMER")
public class DeadLetterConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.chat.messages}")
    private String chatTopic;

    /**
     * Listens to the DLQ (Dead Letter Queue) of the chat-messages flow.
     * Automatically replays the message back to the main flow after 5 minutes.
     */
    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}.DLT", groupId = "dead-letter-recovery-group")
    public void processChatDlt(Object messagePayload) {
        log.warn("🚨 ALARM: Data detected in DLT (Dead Letter Topic)! Database might be down.");

        try {
            // Sleep for 5 minutes before retrying to prevent an infinite loop.
            // If the Database is still down, the main flow will throw it back into DLT,
            // and this consumer will sleep for another 5 minutes before trying again.
            // => The system automatically "buys time" safely until the DB recovers!
            log.info("⏳ Waiting 5 minutes before resurrecting data...");
            TimeUnit.MINUTES.sleep(5);

            log.info("♻️ Replaying data back to main topic: {}", chatTopic);
            kafkaTemplate.send(chatTopic, messagePayload);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DLT Consumer interrupted: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error resurrecting data from DLT: {}", e.getMessage());
        }
    }
}
