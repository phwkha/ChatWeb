package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSystemRequest {
    @NotBlank(message = "{valid.content_empty}")
    private String content;

    private Long survivalTime;
}
