package com.web.backend.controller.websocket;

import com.web.backend.common.NotificationsStatus;
import com.web.backend.controller.response.NotificationMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-WEBSOCKET-CONTROLLER")
public class FriendWebSocketController {

    private final FriendService friendService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/friend/request")
    public void handleFriendRequest(@Payload @NonNull String targetUsername, @NonNull Authentication auth) {
        UserEntity sender = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(sender.getUsername());
        try {
            friendService.sendFriendRequest(username, targetUsername);

            NotificationMessageResponse data = NotificationMessageResponse.builder()
                    .status(NotificationsStatus.REQUEST_SENT_SUCCESS)
                    .relatedUsername(targetUsername)
                    .build();
            simpMessagingTemplate.convertAndSendToUser(
                    username, "/queue/notifications",
                    SocketResponse.notifications("Đã gửi lời mời kết bạn", data));
        } catch (Exception e) {
            sendError(username, e.getMessage());
        }
    }

    @MessageMapping("/friend/accept")
    public void handleAcceptRequest(@Payload @NonNull String requesterUsername, @NonNull Authentication auth) {
        UserEntity acceptor = (UserEntity) auth.getPrincipal();
        String username = Objects.requireNonNull(acceptor.getUsername());
        try {
            friendService.acceptFriendRequest(username, requesterUsername);

            NotificationMessageResponse data = NotificationMessageResponse.builder()
                    .status(NotificationsStatus.YOU_ACCEPTED)
                    .relatedUsername(requesterUsername)
                    .build();
            simpMessagingTemplate.convertAndSendToUser(
                    username, "/queue/notifications",
                    SocketResponse.notifications("Đã chấp nhận kết bạn", data));
        } catch (Exception e) {
            sendError(username, e.getMessage());
        }
    }

    private void sendError(@NonNull String username, String msg) {
        simpMessagingTemplate.convertAndSendToUser(username, "/queue/errors", SocketResponse.error(msg != null ? msg : "Unknown error", username));
    }
}