package com.web.backend.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.web.backend.kafka.payload.EmailPayload;

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-KAFKA-PRODUCER")
public class EmailProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.email.email-topic}")
    private String EMAIL_TOPIC;

    public void sendOtpEmailTask(String to, String name, String otp) {
        log.info("Đang đẩy task gửi mail OTP lên Kafka cho email: {}", to);
        EmailPayload event = EmailPayload.createOtpEvent(to, name, otp);
        kafkaTemplate.send(Objects.requireNonNull(EMAIL_TOPIC), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Lỗi nghiêm trọng: Không thể đẩy message lên Kafka. Topic: {}", EMAIL_TOPIC, ex);
            } else {
                log.debug("Email otp: Push Kafka thành công offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("Đã đẩy task gửi mail OTP lên Kafka Topic '{}' cho email: {}", EMAIL_TOPIC, to);
    }

    public void sendTextEmailTask(String to, String subject, String content) {
        log.info("Đang đẩy task gửi mail TEXT lên Kafka cho email: {}", to);
        EmailPayload event = EmailPayload.createTextEvent(to, subject, content);
        kafkaTemplate.send(Objects.requireNonNull(EMAIL_TOPIC), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Lỗi nghiêm trọng: Không thể đẩy message lên Kafka. Topic: {}", EMAIL_TOPIC, ex);
            } else {
                log.debug("Email text: Push Kafka thành công offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("Đã đẩy task gửi mail TEXT lên Kafka Topic '{}' cho email: {}", EMAIL_TOPIC, to);
    }
}
