package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveKeyRequest {
    @NotBlank(message = "{valid.key_empty}")
    private String key;
}