package com.web.backend.kafka.consumer;

import com.web.backend.model.ChatMessage;
import com.web.backend.repository.MessageRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DATABASE-WRITE-BEHIND-CONSUMER")
public class DatabaseWriteBehindConsumer {

    private final MessageRepository messageRepository;

    @KafkaListener(topics = "${spring.kafka.topic.chat.messages-save.topic}", groupId = "${spring.kafka.topic.chat.messages-save.group-id}", containerFactory = "batchFactory")
    public void handleDbPersistence(List<ChatMessage> messages) {
        log.info("Kafka Consumer: Đang ghi đợt {} tin nhắn xuống Database...", messages.size());
        try {
            messageRepository.saveAll(messages);
            log.info("Đã lưu thành công {} tin nhắn.", messages.size());
        } catch (Exception e) {
            log.error("Lỗi khi ghi dữ liệu ngầm xuống DB: {}", e.getMessage());
        }
    }
}
