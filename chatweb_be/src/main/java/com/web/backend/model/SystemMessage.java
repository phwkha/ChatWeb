package com.web.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("system_message")
@Data
public class SystemMessage {
    @Id
    private String id;

    private String sender;

    private String content;

    private LocalDateTime timestamp;
}

