package com.web.backend.controller;

import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
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

    @MessageMapping("/chat/addUser")
    @SendTo("/topic/public")
    public ChatMessageResponse addUser(@Payload ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Request add user: {}", userPrincipal.getUsername());

        ChatMessage chatMessage = messageMapper.toEntity(request);
        chatMessage.setSender(userPrincipal.getUsername());

        if (userService.userExists(chatMessage.getSender())) {

            normalizeMessage(chatMessage);
            chatMessage.setLocalId(request.getLocalId());

            ChatMessage savedMessage = messageService.save(chatMessage);
            return messageMapper.toResponse(savedMessage);
        }
        return null;
    }

    @MessageMapping("/chat/sendMessage")
    @SendTo("/topic/public")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ChatMessageResponse sendMessage(@Payload ChatMessageRequest request, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        String currentUsername = userPrincipal.getUsername();

        log.debug("Public chat from: {}", currentUsername);

        if (userService.userExists(currentUsername)) {
            if (request.getMessageType() == MessageType.CHAT) {

                ChatMessage chatMessage = messageMapper.toEntity(request);

                chatMessage.setSender(currentUsername);

                normalizeMessage(chatMessage);
                chatMessage.setId(null);
                chatMessage.setStatus(MessageStatus.SENT);
                chatMessage.setLocalId(request.getLocalId());

                ChatMessage savedMessage = messageService.save(chatMessage);
                return messageMapper.toResponse(savedMessage);
            }
        }
        return null;
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessageRequest request, Authentication authentication) {

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

                ChatMessage chatMessage = messageMapper.toEntity(request);
                chatMessage.setSender(senderUsername);

                normalizeMessage(chatMessage);
                chatMessage.setMessageType(MessageType.PRIVATE_CHAT);
                chatMessage.setId(null);
                chatMessage.setStatus(MessageStatus.SENT);
                chatMessage.setLocalId(request.getLocalId());
                messageService.save(chatMessage);

        } catch (Exception e) {
            log.error("Error sending private message: {}", e.getMessage());
            handleChatException(senderUsername, request, e);
        }
    }

    private void handleChatException(String username, ChatMessageRequest request, Exception e) {
        simpMessagingTemplate.convertAndSendToUser(username,
                "/queue/errors", SocketResponse.error(e.getMessage(), request));
    }

    private void normalizeMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        if (chatMessage.getContent() == null) {
            chatMessage.setContent("");
        }
    }
}