package com.web.backend.controller.websocket;

import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;



import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-WEBSOCKET-CONTROLLER")
public class FriendWebSocketController {

    private final FriendService friendService;

    @MessageMapping("/friend/request")
    public void handleFriendRequest(@Payload @NonNull String targetUsername, @NonNull Authentication auth) {
        UserEntity sender = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(sender.getUsername());
        friendService.sendFriendRequest(username, targetUsername);
    }

    @MessageMapping("/friend/accept")
    public void handleAcceptRequest(@Payload @NonNull String requesterUsername, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        friendService.acceptFriendRequest(username, requesterUsername);
    }

    @MessageMapping("/friend/decline")
    public void handleDeclineRequest(@Payload @NonNull String requesterUsername, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        friendService.deleteFriendship(username, requesterUsername);
    }
}