package com.web.backend.controller.response;

import com.web.backend.common.MessageType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String content;
    private String sender;
    private String recipient;
    private LocalDateTime timestamp;
    private boolean isRead;
    private MessageType messageType;

    private String iv;
    private String wrappedKeyRecipient;
    private String wrappedKeySender;

    private String localId;
}