package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class SavePublicKeyRequest {
    @NotBlank(message = "Key cannot be empty")
    private String publicKey;
}
