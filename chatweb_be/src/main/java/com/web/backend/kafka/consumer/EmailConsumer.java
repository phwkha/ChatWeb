package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.web.backend.kafka.payload.EmailPayload;
import com.web.backend.service.util.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.support.Acknowledgment;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-KAFKA-CONSUMER")
public class EmailConsumer {

    private final EmailService emailService;

    private static final String EMAILKAFKALISTENERCONTAINERFACTORY_STRING = "emailKafkaListenerContainerFactory";

    private static final String OTP_STRING = "OTP";

    private static final String TEXT_STRING = "TEXT";

    @KafkaListener(topics = "${spring.kafka.topic.email.email-topic}", groupId = "${spring.kafka.topic.email.group-id}", containerFactory = EMAILKAFKALISTENERCONTAINERFACTORY_STRING)
    public void consumeEmailTask(EmailPayload event, Acknowledgment ack) {
        log.info("Kafka Consumer received email task of type {} for: {}", event.getType(), event.getTo());

        if (OTP_STRING.equals(event.getType())) {
            emailService.sendOtpEmail(event.getTo(), event.getName(), event.getOtp());
        } else if (TEXT_STRING.equals(event.getType())) {
            emailService.sendTextEmail(event.getTo(), event.getSubject(), event.getContent());
        }

        // Manual commit offset sau khi xử lý thành công
        ack.acknowledge();
    }
}
