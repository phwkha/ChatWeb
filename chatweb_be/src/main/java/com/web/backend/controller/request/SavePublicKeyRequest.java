package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SavePublicKeyRequest {
    @NotBlank(message = "{valid.key_empty}")
    private String publicKey;
}
