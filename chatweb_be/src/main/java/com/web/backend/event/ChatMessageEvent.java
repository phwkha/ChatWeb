package com.web.backend.event;

import com.web.backend.controller.response.ChatMessageResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private ChatMessageResponse chatMessageResponse;

    private String senderUsername;

    private String recipientUsername;

}
