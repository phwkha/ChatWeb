package com.web.backend.controller.websocket;

import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final MessageService messageService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/chat/sendMessageSystem")
    @PreAuthorize("hasAuthority('ADMIN_SEND-MESSAGE')")
    public void sendMessage(@Payload @Valid MessageSystemRequest request,
            Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String currentUsername = userPrincipal.getUsername();

        try {
            log.debug("Public chat from: {}", currentUsername);
            messageService.sendSystemMessage(currentUsername, request);
        } catch (Exception e) {
            log.error("Error sending system message: {}", e.getMessage());
            handleChatException(currentUsername, request, "Lỗi không thể nhắn tin hệ thống");
        }
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload @Valid ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        try {
            log.debug("Private from {} to {}", senderUsername, request.getRecipient());

            messageService.sendPrivateMessage(senderUsername, request);

        } catch (Exception e) {
            log.error("Error sending private message: {}", e.getMessage());
            handleChatException(senderUsername, request, e.getMessage());
        }
    }

    private void handleChatException(String username, Object request, String mes) {
        if (username == null) {
            simpMessagingTemplate.convertAndSendToUser("unknows",
                    "/queue/errors", SocketResponse.error(mes, request));
        } else {
            simpMessagingTemplate.convertAndSendToUser(username,
                    "/queue/errors", SocketResponse.error(mes, request));
        }
    }
}