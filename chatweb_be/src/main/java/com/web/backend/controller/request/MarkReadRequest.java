package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkReadRequest {
    @NotBlank(message = "{valid.sender_empty}")
    private String sender;
}