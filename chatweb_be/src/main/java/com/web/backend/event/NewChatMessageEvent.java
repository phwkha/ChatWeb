package com.web.backend.event;

import com.web.backend.controller.response.ChatMessageResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewChatMessageEvent extends ApplicationEvent {
    private final ChatMessageResponse response;
    private final String senderUsername;
    private final String recipientUsername;

    public NewChatMessageEvent(Object source, ChatMessageResponse response, String senderUsername, String recipientUsername) {
        super(source);
        this.response = response;
        this.senderUsername = senderUsername;
        this.recipientUsername = recipientUsername;
    }
}