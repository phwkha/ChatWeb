package com.web.backend.controller;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT-CONTROLLER")
public class ChatController {

    private final UserService userService;
    private final MessageService messageService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/chat/addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Request add user: {}", chatMessage.getSender());

        if (userService.userExists(chatMessage.getSender())) {
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            }

            userService.setUserOnlineStatus(chatMessage.getSender(), true);
            log.info("User added successfully: {} (Session ID: {})",
                    chatMessage.getSender(), headerAccessor.getSessionId());

            normalizeMessage(chatMessage);

            ChatMessage savedMessage = messageService.save(chatMessage);
            log.debug("Saved JOIN message: {}", savedMessage.getId());
            return savedMessage;
        }

        log.warn("Add user failed: User {} does not exist", chatMessage.getSender());
        return null;
    }

    @MessageMapping("/chat/sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        log.debug("Public chat from: {}", chatMessage.getSender());

        if (userService.userExists(chatMessage.getSender())) {
            if (chatMessage.getMessageType() == MessageType.CHAT) {
                normalizeMessage(chatMessage);
                return messageService.save(chatMessage);
            }
            return chatMessage;
        }

        log.warn("Public chat rejected: Sender {} not found", chatMessage.getSender());
        return null;
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        log.info("Private message (E2EE) from {} to {}", chatMessage.getSender(), chatMessage.getRecipient());

        if (userService.userExists(chatMessage.getSender()) && userService.userExists(chatMessage.getRecipient())) {

            normalizeMessage(chatMessage);
            chatMessage.setMessageType(MessageType.PRIVATE_CHAT);
            chatMessage.setId(null);

            ChatMessage savedMessage = messageService.save(chatMessage);
            log.debug("Private message saved with ID: {}", savedMessage.getId());

            try {
                String recipientDestination = "/user/" + chatMessage.getRecipient() + "/queue/private";
                simpMessagingTemplate.convertAndSend(recipientDestination, savedMessage);

                String senderDestination = "/user/" + chatMessage.getSender() + "/queue/private";
                simpMessagingTemplate.convertAndSend(senderDestination, savedMessage);

            } catch (Exception e) {
                log.error("Error sending private message from {} to {}",
                        chatMessage.getSender(), chatMessage.getRecipient(), e);
            }
        } else {
            log.error("Send private message failed: Sender or Recipient not found");
        }
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