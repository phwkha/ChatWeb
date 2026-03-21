package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.web.backend.event.EmailEvent;
import com.web.backend.service.util.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-KAFKA-CONSUMER")
public class EmailConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "${spring.kafka.topic.email.email-topic}", groupId = "${spring.kafka.topic.email.group-id}")
    public void consumeEmailTask(EmailEvent event) {
        log.info("Kafka Consumer nhận được task gửi mail loại {} cho: {}", event.getType(), event.getTo());

        try {
            if ("OTP".equals(event.getType())) {
                emailService.sendOtpEmail(event.getTo(), event.getName(), event.getOtp());
            } else if ("TEXT".equals(event.getType())) {
                emailService.sendTextEmail(event.getTo(), event.getSubject(), event.getContent());
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý gửi mail từ Kafka cho {}: {}", event.getTo(), e.getMessage());
        }
    }
}
