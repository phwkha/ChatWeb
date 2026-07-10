package com.web.backend.listener;

import com.web.backend.event.FriendshipEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "FRIENDSHIP-EVENT-LISTENER")
public class FriendshipEventListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFriendshipEvent(FriendshipEvent<?> event) {
        if (event == null || event.getRecipientUsername() == null || event.getDestination() == null) {
            log.warn("Nhận được FriendshipEvent không hợp lệ (bị null)");
            return;
        }

        log.info("Sending WebSocket notification to user: {}", event.getRecipientUsername());

        try {
            simpMessagingTemplate.convertAndSendToUser(
                    Objects.requireNonNull(event.getRecipientUsername()),
                    Objects.requireNonNull(event.getDestination()),
                    Objects.requireNonNull(event.getPayload()));
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
        }
    }
}