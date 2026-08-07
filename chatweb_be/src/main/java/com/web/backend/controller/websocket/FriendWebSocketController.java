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
import com.web.backend.controller.request.FriendRequest;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-WEBSOCKET-CONTROLLER")
public class FriendWebSocketController {

    private final FriendService friendService;

    @MessageMapping("/friend/request")
    public void handleFriendRequest(@Payload @jakarta.validation.Valid FriendRequest request, @NonNull Authentication auth) {
        UserEntity sender = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(sender.getUsername());
        friendService.sendFriendRequest(username, request.getTargetUsername());
    }

    @MessageMapping("/friend/accept")
    public void handleAcceptRequest(@Payload @jakarta.validation.Valid FriendRequest request, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        friendService.acceptFriendRequest(username, request.getTargetUsername());
    }

    @MessageMapping("/friend/decline")
    public void handleDeclineRequest(@Payload @jakarta.validation.Valid FriendRequest request, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        friendService.deleteFriendship(username, request.getTargetUsername());
    }
}