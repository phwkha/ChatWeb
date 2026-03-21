package com.web.backend.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.web.backend.event.EmailEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-KAFKA-PRODUCER")
public class EmailProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.email.email-topic}")
    private String EMAIL_TOPIC;

    public void sendOtpEmailTask(String to, String name, String otp) {
        log.info("Đang đẩy task gửi mail OTP lên Kafka cho email: {}", to);
        EmailEvent event = EmailEvent.createOtpEvent(to, name, otp);
        kafkaTemplate.send(EMAIL_TOPIC, event);
        log.info("Đã đẩy task gửi mail OTP lên Kafka Topic '{}' cho email: {}", EMAIL_TOPIC, to);
    }

    public void sendTextEmailTask(String to, String subject, String content) {
        log.info("Đang đẩy task gửi mail TEXT lên Kafka cho email: {}", to);
        EmailEvent event = EmailEvent.createTextEvent(to, subject, content);
        kafkaTemplate.send(EMAIL_TOPIC, event);
        log.info("Đã đẩy task gửi mail TEXT lên Kafka Topic '{}' cho email: {}", EMAIL_TOPIC, to);
    }
}
