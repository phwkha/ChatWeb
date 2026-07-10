package com.web.backend.listener;

import com.web.backend.event.KafkaDispatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-DISPATCHER-LISTENER")
public class KafkaDispatcherListener {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleKafkaDispatch(KafkaDispatchEvent event) {
        if (event == null || event.topic() == null || event.payload() == null) {
            return;
        }
        try {
            kafkaTemplate.send(Objects.requireNonNull(event.topic(), "Topic must not be null"), event.payload());
            log.info("Đã dispatch Kafka message lên topic: {}", event.topic());
        } catch (Exception e) {
            log.error("Lỗi khi dispatch Kafka message: {}", e.getMessage(), e);
        }
    }
}
