package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FriendRequest {
    @NotBlank(message = "Username must not be blank")
    private String targetUsername;
}
