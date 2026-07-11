package com.web.backend.kafka.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;
import com.web.backend.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DATABASE-WRITE-BEHIND-CONSUMER")
public class DatabaseWriteBehindConsumer {

    private final MessageRepository messageRepository;

    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}", groupId = "${spring.kafka.topic.chat.save}", containerFactory = "batchFactory")
    public void handleDbPersistence(List<ChatMessage> messages) {
        List<ChatMessage> messagesToSave = messages.stream().filter(msg -> msg.getMessageType() == MessageType.CHAT)
                .toList();
        if (messagesToSave.isEmpty()) {
            return;
        }
        log.info("Kafka Consumer: Writing batch of {} messages to Database...", messages.size());
        int maxAttempts = 3;
        long backoffDelay = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                messageRepository.saveAll(messagesToSave);
                log.info("Successfully saved {} messages.", messages.size());
                return;
            } catch (Exception e) {
                log.warn("Error writing to DB (Attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Database save FAILED after {} attempts. Skipping this message batch.", maxAttempts);
                } else {
                    try {
                        Thread.sleep(backoffDelay);
                        backoffDelay *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
