package com.web.backend.model;

import com.web.backend.common.MessageType;
import com.web.backend.common.MessageStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document("messages")
@Data
@CompoundIndex(name = "unread_msg_idx", def = "{'recipient': 1, 'status': 1, 'messageType': 1}")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private String sender;
    @Indexed
    private String recipient;

    private String content;
    private MessageType messageType;
    private String color;

    private String replyToId;

    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private LocalDateTime timestamp;

    private MessageStatus status;

    private boolean isEdited;
    private boolean isDeleted;

    private String iv;
    private String wrappedKeyRecipient;
    private String wrappedKeySender;

    private Map<String, String> reactions;

    @org.springframework.data.annotation.Transient
    private String localId;
}