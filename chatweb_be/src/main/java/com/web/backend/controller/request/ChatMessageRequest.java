package com.web.backend.controller.request;

import com.web.backend.common.MessageType;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private String content;
    private String recipient;
    private MessageType messageType;

    private String iv;
    private String wrappedKeyRecipient;
    private String wrappedKeySender;

    private String localId;
}