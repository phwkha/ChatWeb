package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FriendRequest {
    @NotBlank(message = "{valid.username_empty}")
    private String targetUsername;
}
