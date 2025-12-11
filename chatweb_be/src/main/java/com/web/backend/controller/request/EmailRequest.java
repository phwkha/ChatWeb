package com.web.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {
    @NotBlank(message = "Người nhận (to) không được để trống")
    @Email(message = "Địa chỉ email không hợp lệ")
    private String to;

    @NotBlank(message = "Tiêu đề (subject) không được để trống")
    private String subject;

    @NotBlank(message = "Nội dung (text) không được để trống")
    private String text;
}
