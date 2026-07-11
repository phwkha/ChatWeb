package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSystemRequest {
    @NotBlank(message = "{valid.new_email_empty}")
    private String content;

    private Long survivalTime;
}
