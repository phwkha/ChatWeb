package com.web.backend.event;

public record KafkaDispatchEvent(String topic, Object payload) {}
