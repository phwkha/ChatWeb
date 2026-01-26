package com.web.backend.controller.websocket;

import com.web.backend.common.ContentType;
import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final UserService userService;

    private final MessageService messageService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final MessageMapper messageMapper;

    private final FriendService friendService;

    @MessageMapping("/chat/sendMessageSystem")
    @SendTo("/topic/public")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public MessageSystemResponse sendMessage(@Payload @Valid MessageSystemRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String currentUsername = userPrincipal.getUsername();

        try {
                log.debug("Public chat from: {}", currentUsername);

                SystemMessage systemMessage = new SystemMessage();
                systemMessage.setSender(currentUsername);
                systemMessage.setContent(request.getContent());
                messageService.saveSystemMessage(systemMessage);
                return MessageSystemResponse.builder()
                        .sender(systemMessage.getSender())
                        .content(systemMessage.getContent())
                        .build();

        } catch (Exception e) {
            log.error("Error sending system message: {}", e.getMessage());
            handleChatException(currentUsername, request, "Lỗi không thể nhắn tin hệ thống");
        }
        return null;
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload @Valid ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String senderUsername = userPrincipal.getUsername();

        try {
            log.info("Private from {} to {}", senderUsername, request.getRecipient());

            if ( !userService.userExists(request.getRecipient())) {
                throw new ResourceNotFoundException("Người nhận không tồn tại");
            }

            if (!friendService.isFriend(senderUsername, request.getRecipient())) {
                throw new AccessForbiddenException("Hai người chưa kết bạn, không thể nhắn tin.");
            }

            sendMessage(senderUsername, request);

        } catch (Exception e) {
            log.error("Error sending private message: {}", e.getMessage());
            handleChatException(senderUsername, request, e.getMessage());
        }
    }

    private void handleChatException(String username, Object request, String mes) {
        simpMessagingTemplate.convertAndSendToUser(username,
                "/queue/errors", SocketResponse.error(mes, request));
    }

    private void normalizeMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        if (chatMessage.getContent() == null) {
            chatMessage.setContent("");
        }
    }

    private void sendMessage(String senderUsername, ChatMessageRequest request) {
        switch (request.getMessageType()) {
            case CHAT -> {
                ChatMessage chatMessage = messageMapper.toEntity(request);
                chatMessage.setSender(senderUsername);

                normalizeMessage(chatMessage);
                if (chatMessage.getContentType() == null) chatMessage.setContentType(ContentType.TEXT);
                chatMessage.setId(null);
                chatMessage.setStatus(MessageStatus.SENT);
                chatMessage.setLocalId(request.getLocalId());
                messageService.saveMessage(chatMessage);
            }
            case TYPING -> {
                ChatMessage chatMessage = messageMapper.toEntity(request);
                messageService.messageTyping(chatMessage);
            }
            default -> log.warn("Unknown status");
        }
    }
}