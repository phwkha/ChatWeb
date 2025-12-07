package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkReadRequest {
    @NotBlank(message = "Sender username cannot be empty")
    private String sender;
}