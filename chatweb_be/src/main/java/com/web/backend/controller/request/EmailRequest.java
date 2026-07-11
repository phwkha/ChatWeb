package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {
    @NotBlank(message = "{valid.to_empty}")
    @Email(message = "{valid.email_addr_invalid}")
    private String to;

    @NotBlank(message = "{valid.subject_empty}")
    private String subject;

    @NotBlank(message = "{valid.text_empty}")
    private String text;
}
