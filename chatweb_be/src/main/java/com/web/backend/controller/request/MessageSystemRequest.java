package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSystemRequest {
    @NotBlank(message = "Email mới không được để trống")
    private String content;
}
