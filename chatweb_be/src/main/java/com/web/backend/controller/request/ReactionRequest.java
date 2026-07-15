package com.web.backend.controller.request;

import com.web.backend.common.ReactionType;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank(message = "{valid.msg_id_empty}")
    private String messageId;

    @NotBlank(message = "{valid.recipient_empty}")
    private String recipient;

    private ReactionType reactionType;
}
