package com.web.backend.controller.request;

import com.web.backend.common.ContentType;
import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import lombok.Data;

import java.util.Map;

@Data
public class ChatMessageRequest {
    private String recipient;

    private String content;
    private ContentType contentType;
    private MessageType messageType;
    private String color;

    private String replyToId;

    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private MessageStatus status;

    private boolean isEdited;
    private boolean isDeleted;

    private Map<String, String> reactions;

    private String iv;
    private String wrappedKeyRecipient;
    private String wrappedKeySender;

    private String localId;
}