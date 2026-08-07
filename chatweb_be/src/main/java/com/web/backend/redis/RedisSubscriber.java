package com.web.backend.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public void receiveMessage(String message) {
        try {
            RedisWsMessage wsMessage = objectMapper.readValue(message, RedisWsMessage.class);
            simpMessagingTemplate.convertAndSendToUser(wsMessage.getRecipient(), wsMessage.getDestination(),
                    wsMessage.getPayload());
            log.info("Routed WebSocket message to {} via Redis Pub/Sub", wsMessage.getRecipient());
        } catch (Exception e) {
            log.error("Error processing Redis WebSocket message: {}", e.getMessage(), e);
        }
    }
}
