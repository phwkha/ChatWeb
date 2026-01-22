package com.web.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.Map;

@Getter
public class FriendshipEvent<T> extends ApplicationEvent {
    private final String recipientUsername;
    private final String destination;
    private final T payload;

    public FriendshipEvent(Object source, String recipientUsername, String destination, T payload) {
        super(source);
        this.recipientUsername = recipientUsername;
        this.destination = destination;
        this.payload = payload;
    }
}