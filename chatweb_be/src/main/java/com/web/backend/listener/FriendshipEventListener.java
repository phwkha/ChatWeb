package com.web.backend.listener;

import com.web.backend.event.FriendshipEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendshipEventListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFriendshipEvent(FriendshipEvent event) {
        log.info("Sending WebSocket notification to user: {}", event.getRecipientUsername());

        try {
            simpMessagingTemplate.convertAndSendToUser(
                    event.getRecipientUsername(),
                    event.getDestination(),
                    event.getPayload()
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }
}