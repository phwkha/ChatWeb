package com.web.backend.controller.websocket;

import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.request.ReactionRequest;
import com.web.backend.model.UserEntity;
import com.web.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final MessageService messageService;

    @MessageMapping("/chat/sendMessageSystem")
    @PreAuthorize("hasAuthority('ADMIN_SEND-MESSAGE')")
    public void sendMessage(@Payload @Valid MessageSystemRequest request,
            Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String currentUsername = userPrincipal.getUsername();

        log.debug("Public chat from: {}", currentUsername);
        messageService.sendSystemMessage(currentUsername, request);
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload @Valid ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        log.debug("Private from {} to {}", senderUsername, request.getRecipient());
        messageService.sendPrivateMessage(senderUsername, request);
    }

    @MessageMapping("/chat/reaction")
    public void reactToMessage(@Payload @Valid ReactionRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        log.debug("Reaction from {} to message {} of {}",
                senderUsername, request.getMessageId(), request.getRecipient());
        messageService.reactToMessage(senderUsername, request);
    }

    @MessageMapping("/chat/editMessage")
    public void editMessage(@Payload @Valid com.web.backend.controller.request.EditMessageRequest request,
            Authentication authentication) {
        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        log.debug("Edit message {} from {}", request.getMessageId(), senderUsername);
        messageService.editMessage(senderUsername, request);
    }

    @MessageMapping("/chat/revokeMessage")
    public void revokeMessage(@Payload @Valid com.web.backend.controller.request.RevokeMessageRequest request,
            Authentication authentication) {
        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        log.debug("Revoke message {} from {}", request.getMessageId(), senderUsername);
        messageService.revokeMessage(senderUsername, request);
    }

}