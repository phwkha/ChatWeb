package com.web.backend.listener;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. Thêm cái này
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketListener {

    private final UserService userService;

    private final SimpMessageSendingOperations messagingTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "online_users";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getFirstNativeHeader("username");

        if (username != null) {
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);

            headerAccessor.getSessionAttributes().put("username", username);

            log.info("User Online (Redis): {}", username);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Object usernameObj = accessor.getSessionAttributes().get("username");

        if (usernameObj != null) {
            String username = usernameObj.toString();

            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);

            log.info("User Disconnected: {}", username);

            userService.setUserOnlineStatus(username, false);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageType(MessageType.LEAVE);
            chatMessage.setSender(username);
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        } else {
            log.warn("Disconnected session with no username attribute.");
        }
    }
}