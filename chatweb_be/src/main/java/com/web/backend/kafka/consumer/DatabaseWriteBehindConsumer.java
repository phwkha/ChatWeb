package com.web.backend.kafka.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
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

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = "-dlt")
    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}", groupId = "${spring.kafka.topic.chat.save.chat-save}", containerFactory = "batchFactory")
    public void handleDbPersistence(List<ChatMessage> messages) {
        List<ChatMessage> messagesToSave = messages.stream().filter(msg -> msg.getMessageType() == MessageType.CHAT)
                .toList();
        if (messagesToSave.isEmpty()) {
            return;
        }
        log.info("Kafka Consumer: Đang ghi đợt {} tin nhắn xuống Database...", messages.size());
        try {
            messageRepository.saveAll(messagesToSave);
            log.info("Đã lưu thành công {} tin nhắn.", messages.size());
        } catch (Exception e) {
            log.error("Lỗi khi ghi dữ liệu ngầm xuống DB: {}", e.getMessage());
        }
    }
}
