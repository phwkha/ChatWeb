package com.web.backend.controller.request;

import com.web.backend.common.ContentType;
import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class ChatMessageRequest {
    @NotBlank(message = "{valid.recipient_empty}")
    private String recipient;

    @Size(max = 10000, message = "{valid.msg_max_10000}")
    private String content;

    private ContentType contentType;

    @NotNull(message = "{valid.msg_type_empty}")
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