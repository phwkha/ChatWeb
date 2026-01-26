package com.web.backend.repository.projection;

import lombok.Data;

@Data
public class UnreadCountProjection {
    private String sender;
    private Long count;
}