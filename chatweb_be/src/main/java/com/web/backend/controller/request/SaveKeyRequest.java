package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveKeyRequest {
    @NotBlank(message = "Key cannot be empty")
    private String key;
}