package com.web.backend.controller;

import com.web.backend.common.MessageType;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final UserService userService;

    private final MessageService messageService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final MessageMapper messageMapper;

    @MessageMapping("/chat/addUser")
    @SendTo("/topic/public")
    public ChatMessageResponse addUser(@Payload ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor, Authentication authentication) {

        UserEntity userPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Request add user: {}", userPrincipal.getUsername());

        ChatMessage chatMessage = messageMapper.toEntity(request);
        chatMessage.setSender(userPrincipal.getUsername());

        if (userService.userExists(chatMessage.getSender())) {
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            }

            userService.setUserOnlineStatus(chatMessage.getSender(), true);
            normalizeMessage(chatMessage);

            ChatMessage savedMessage = messageService.save(chatMessage);
            return messageMapper.toResponse(savedMessage);
        }
        return null;
    }

    @MessageMapping("/chat/sendMessage")
    @SendTo("/topic/public")
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
                chatMessage.setRead(false);

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

        String localId = request.getLocalId();

        try {
            log.info("Private from {} to {}", senderUsername, request.getRecipient());

            if ( !userService.userExists(request.getRecipient())) {
                throw new ResourceNotFoundException("Người nhận không tồn tại");
            }
                ChatMessage chatMessage = messageMapper.toEntity(request);
                chatMessage.setSender(senderUsername);

                normalizeMessage(chatMessage);
                chatMessage.setMessageType(MessageType.PRIVATE_CHAT);
                chatMessage.setId(null);
                chatMessage.setRead(false);

                ChatMessage savedMessage = messageService.save(chatMessage);
                ChatMessageResponse response = messageMapper.toResponse(savedMessage);
                response.setLocalId(localId);

                simpMessagingTemplate.convertAndSendToUser(request.getRecipient(), "/queue/private", response);
                simpMessagingTemplate.convertAndSendToUser(senderUsername, "/queue/private", response);

        } catch (MessagingException e) {
            log.error("Error sending private message: {}", e.getMessage());
            handleChatException(senderUsername, localId, e);
        }
    }

    private void handleChatException(String username, String localId, Exception e) {
        int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = "Lỗi hệ thống không xác định";

        if (e instanceof ResourceNotFoundException) {
            code = HttpStatus.NOT_FOUND.value();
            message = e.getMessage();
        } else if (e instanceof IllegalArgumentException) {
            code = HttpStatus.BAD_REQUEST.value();
            message = e.getMessage();
        } else {
            message = e.getMessage();
        }

        ApiResponse<Map<String, String>> errorResponse = ApiResponse.<Map<String, String>>builder()
                .code(code)
                .status("error")
                .message(message)
                .data(Map.of("localId", localId != null ? localId : ""))
                .build();

        simpMessagingTemplate.convertAndSendToUser(username, "/queue/errors", errorResponse);
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