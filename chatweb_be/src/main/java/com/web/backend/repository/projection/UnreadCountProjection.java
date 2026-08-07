package com.web.backend.repository.projection;

public record UnreadCountProjection(
    String sender,
    Long count
) {}