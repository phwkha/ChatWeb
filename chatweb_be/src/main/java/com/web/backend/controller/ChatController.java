package com.web.backend.controller;

import com.web.backend.common.MessageType;
import com.web.backend.config.WebSocketConfig;
import com.web.backend.model.ChatMessage;
import com.web.backend.service.MessageService;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final UserService userService;

    private final MessageService messageService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @MessageMapping("/chat/addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {

        System.out.println("addUser" + chatMessage);

        if (userService.userExists(chatMessage.getSender())){

            logger.info("User validated successfully: {}", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            userService.setUserOnlineStatus(chatMessage.getSender(), true);

            System.out.println("user added successfully " + chatMessage.getSender() + " with session Id "
            + headerAccessor.getSessionId());

            chatMessage.setTimestamp(LocalDateTime.now());
            if (chatMessage.getContent() == null ) {
                chatMessage.setContent("");
            }
            ChatMessage out = messageService.save(chatMessage);
            logger.info("Saved message: {}", out);
            return out;
        }
        logger.info("addUser called for sender={}, exists={}", chatMessage.getSender(), userService.userExists(chatMessage.getSender()));
        return null;
    }

    @MessageMapping("/chat/sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {

        System.out.println("sendMessage" + chatMessage);

        if (userService.userExists(chatMessage.getSender())){
            System.out.println("user exists for sender=" + chatMessage.getSender());
            if (chatMessage.getMessageType() == MessageType.CHAT) {
                if (chatMessage.getTimestamp() == null) {
                    chatMessage.setTimestamp(LocalDateTime.now());
                }
                if (chatMessage.getContent() == null) {
                    chatMessage.setContent("");
                }
                return messageService.save(chatMessage);
            }
            return chatMessage;
        }
        return null;
    }

    @MessageMapping("/chat/sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {

        System.out.println("sendPrivateMessage (E2EE)" + chatMessage.getSender() + " to " + chatMessage.getRecipient());

        if (userService.userExists(chatMessage.getSender()) && userService.userExists(chatMessage.getRecipient())) {

            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }

            if (chatMessage.getContent() == null) {
                chatMessage.setContent("");
            }

            chatMessage.setMessageType(MessageType.PRIVATE_CHAT);

            chatMessage.setId(null);

            ChatMessage savedMessage = messageService.save(chatMessage);
            System.out.println("Message saved successfully with Id" + savedMessage.getId());

            try {
                String recipientDestination = "/user/" + chatMessage.getRecipient() + "/queue/private";
                System.out.println("Sending message to recipient destination" + recipientDestination);
                simpMessagingTemplate.convertAndSend(recipientDestination, savedMessage);

                String senderDestination = "/user/" + chatMessage.getSender() + "/queue/private";
                System.out.println("Sending message to sender destination" + senderDestination);
                simpMessagingTemplate.convertAndSend(senderDestination, savedMessage);
            }  catch (Exception e) {
                System.out.println("ERROR occured while sending the message" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ERROR: sender " +  chatMessage.getSender() + "or recipient" + chatMessage.getRecipient() + " does not exist");
        }
    }

}
