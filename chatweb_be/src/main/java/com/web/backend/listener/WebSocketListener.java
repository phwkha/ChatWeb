package com.web.backend.listener;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketListener {

    private final UserService userService;

    private final SimpMessageSendingOperations messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketListener.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Lấy username từ header "username" mà client đã gửi trong lệnh CONNECT
        String username = headerAccessor.getFirstNativeHeader("username");

        if (username != null) {
            // Log này sẽ chạy trước cả log "addUser"
            System.out.println("Client connected with username header: " + username);

            // Bạn có thể put nó vào session ở đây nếu muốn
            // headerAccessor.getSessionAttributes().put("username", username);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Object usernameObj = accessor.getSessionAttributes().get("username");

        if (usernameObj != null) {
            String username = usernameObj.toString();
            System.out.println("Disconnected from web socket: " + username);
            userService.setUserOnlineStatus(username, false);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageType(MessageType.LEAVE);
            chatMessage.setSender(username);
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        } else {
            logger.warn("Disconnected session with no username attribute.");
        }
    }
}
