package com.web.backend.model;


import com.web.backend.common.MessageType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document("messages")
@Data
public class ChatMessage {

    @Id
    private String id;

    private String content;

    private String sender;

    private String recipient;

    private String color;

    private LocalDateTime timestamp;

    @Field("is_read")
    private boolean isRead;

    private MessageType messageType;

    private String iv; // Vector khởi tạo (IV) cho mã hóa AES-GCM

    private String wrappedKeyRecipient; // Khóa AES (đã wrap) cho người nhận
    private String wrappedKeySender; // Khóa AES (đã wrap) cho người gửi

    @org.springframework.data.annotation.Transient
    private String localId;

}
