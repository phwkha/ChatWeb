package com.web.backend.kafka.payload;

import com.web.backend.controller.response.ChatMessageResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {

    private ChatMessageResponse chatMessageResponse;

    private String senderUsername;

    private String recipientUsername;

}
