package com.web.backend.controller.response;

import lombok.Data;

@Data
public class UnreadCountResultResponse {
    private String senderId;
    private Long count;
}