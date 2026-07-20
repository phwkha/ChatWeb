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
import com.web.backend.exception.WebSocketErrorHandler;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.InvalidDataException;
import com.web.backend.exception.custom.ResourceConflictException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.config.LocalResolverConfig.Translator;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-WEBSOCKET-CONTROLLER")
public class FriendWebSocketController {

    private final FriendService friendService;

    private final WebSocketErrorHandler webSocketErrorHandler;

    private static final String ERROR_SYS_BUSY_STRING = "error.sys.busy";

    @MessageMapping("/friend/request")
    public void handleFriendRequest(@Payload @NonNull String targetUsername, @NonNull Authentication auth) {
        UserEntity sender = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(sender.getUsername());
        try {
            friendService.sendFriendRequest(username, targetUsername);

        } catch (AccessForbiddenException | ResourceNotFoundException | ResourceConflictException
                | InvalidDataException e) {
            log.warn("Business error handling friend request: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(username, targetUsername, e.getMessage());
        } catch (Exception e) {
            log.error("System error handling friend request: ", e);
            webSocketErrorHandler.handleChatError(username, targetUsername, Translator.tolocale(ERROR_SYS_BUSY_STRING));
        }
    }

    @MessageMapping("/friend/accept")
    public void handleAcceptRequest(@Payload @NonNull String requesterUsername, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        try {
            friendService.acceptFriendRequest(username, requesterUsername);

        } catch (AccessForbiddenException | ResourceNotFoundException | ResourceConflictException
                | InvalidDataException e) {
            log.warn("Business error accepting friend request: {}", e.getMessage());
            webSocketErrorHandler.handleChatError(username, requesterUsername, e.getMessage());
        } catch (Exception e) {
            log.error("System error accepting friend request: ", e);
            webSocketErrorHandler.handleChatError(username, requesterUsername,
                    Translator.tolocale(ERROR_SYS_BUSY_STRING));
        }
    }

}