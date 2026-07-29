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
    private String emailTopic;

    public void sendOtpEmailTask(String to, String name, String otp) {
        log.info("Pushing OTP email task to Kafka for email: {}", to);
        EmailPayload event = EmailPayload.createOtpEvent(to, name, otp);
        kafkaTemplate.send(Objects.requireNonNull(emailTopic), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Critical Error: Cannot push message to Kafka. Topic: {}", emailTopic, ex);
            } else {
                log.debug("Email otp: Kafka push successful offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("Pushed OTP email task to Kafka Topic '{}' for email: {}", emailTopic, to);
    }

    public void sendTextEmailTask(String to, String subject, String content) {
        log.info("Pushing TEXT email task to Kafka for email: {}", to);
        EmailPayload event = EmailPayload.createTextEvent(to, subject, content);
        kafkaTemplate.send(Objects.requireNonNull(emailTopic), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Critical Error: Cannot push message to Kafka. Topic: {}", emailTopic, ex);
            } else {
                log.debug("Email text: Kafka push successful offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("Pushed TEXT email task to Kafka Topic '{}' for email: {}", emailTopic, to);
    }
}
