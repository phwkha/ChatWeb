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
import com.web.backend.exception.WebSocketErrorHandler;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.InvalidDataException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.config.LocalResolverConfig.Translator;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final MessageService messageService;
    private final WebSocketErrorHandler webSocketErrorHandler;

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
            webSocketErrorHandler.handleChatError(currentUsername, request,
                    Translator.tolocale("error.chat.sys_msg_fail"));
        }
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload @Valid ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        try {
            log.debug("Private from {} to {}", senderUsername, request.getRecipient());

            messageService.sendPrivateMessage(senderUsername, request);

        } catch (AccessForbiddenException | ResourceNotFoundException | InvalidDataException e) {
            log.warn("Business error sending private message: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(senderUsername, request, e.getMessage());
        } catch (Exception e) {
            log.error("System error sending private message: ", e);
            webSocketErrorHandler.handleChatError(senderUsername, request, Translator.tolocale("error.sys.busy"));
        }
    }

    @MessageMapping("/chat/reaction")
    public void reactToMessage(@Payload @Valid ReactionRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        try {
            log.debug("Reaction from {} to message {} of {}",
                    senderUsername, request.getMessageId(), request.getRecipient());

            messageService.reactToMessage(senderUsername, request);

        } catch (AccessForbiddenException | ResourceNotFoundException | InvalidDataException e) {
            log.warn("Business error processing reaction: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(senderUsername, request, e.getMessage());
        } catch (Exception e) {
            log.error("System error processing reaction: ", e);
            webSocketErrorHandler.handleChatError(senderUsername, request, Translator.tolocale("error.sys.busy"));
        }
    }

    @MessageMapping("/chat/editMessage")
    public void editMessage(@Payload @Valid com.web.backend.controller.request.EditMessageRequest request, Authentication authentication) {
        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();
        try {
            log.debug("Edit message {} from {}", request.getMessageId(), senderUsername);
            messageService.editMessage(senderUsername, request);
        } catch (AccessForbiddenException | ResourceNotFoundException | InvalidDataException e) {
            log.warn("Business error editing message: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(senderUsername, request, e.getMessage());
        } catch (Exception e) {
            log.error("System error editing message: ", e);
            webSocketErrorHandler.handleChatError(senderUsername, request, Translator.tolocale("error.sys.busy"));
        }
    }

    @MessageMapping("/chat/revokeMessage")
    public void revokeMessage(@Payload @Valid com.web.backend.controller.request.RevokeMessageRequest request, Authentication authentication) {
        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();
        try {
            log.debug("Revoke message {} from {}", request.getMessageId(), senderUsername);
            messageService.revokeMessage(senderUsername, request);
        } catch (AccessForbiddenException | ResourceNotFoundException | InvalidDataException e) {
            log.warn("Business error revoking message: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(senderUsername, request, e.getMessage());
        } catch (Exception e) {
            log.error("System error revoking message: ", e);
            webSocketErrorHandler.handleChatError(senderUsername, request, Translator.tolocale("error.sys.busy"));
        }
    }

}