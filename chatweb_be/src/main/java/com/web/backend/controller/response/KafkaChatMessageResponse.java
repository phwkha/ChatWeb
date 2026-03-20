package com.web.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaChatMessageResponse {

    private ChatMessageResponse chatMessageResponse;

    private String senderUsername;

    private String recipientUsername;

}
