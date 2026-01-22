package com.web.backend.listener;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketListener {

    private final UserService userService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "online_users";

    private static final String ONLINE_USERS_COUNT_KEY = "online_users_count";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = headerAccessor.getUser();
        String username = (user != null) ? user.getName() : null;

        if (username != null) {

            Long count = redisTemplate.opsForHash().increment(ONLINE_USERS_COUNT_KEY, username, 1);

            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);

            if (count != null && count == 1) {
                userService.setUserOnlineStatus(username, true);
                log.info("User Online (First Session): {}", username);
            } else {
                log.debug("User opened new tab/device: {}, total sessions: {}", username, count);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = headerAccessor.getUser();
        String username = (user != null) ? user.getName() : null;

        if (username != null) {

            log.info("WebSocket Disconnected: {}", username);

            Long count = redisTemplate.opsForHash().increment(ONLINE_USERS_COUNT_KEY, username, -1);

            if (count != null && count <= 0) {

                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);

                redisTemplate.opsForHash().delete(ONLINE_USERS_COUNT_KEY, username);

                userService.setUserOnlineStatus(username, false);

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMessageType(MessageType.LEAVE);
                chatMessage.setSender(username);
                messagingTemplate.convertAndSend("/topic/public", chatMessage);

                log.info("User Disconnected Completely (All sessions closed): {}", username);
            } else {
                log.info("User closed one session, still online on other devices: {}, remaining: {}", username, count);
            }
        }
    }
}