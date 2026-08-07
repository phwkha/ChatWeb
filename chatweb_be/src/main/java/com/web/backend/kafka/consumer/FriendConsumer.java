package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.backend.redis.RedisWsMessage;
import com.web.backend.config.ServerIdentity;

import com.web.backend.common.NotificationsType;
import com.web.backend.config.localresolverconfig.Translator;
import com.web.backend.controller.response.NotificationMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.kafka.payload.FriendPayload;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-KAFKA-CONSUMER")
public class FriendConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String DESTINATION_MUST_NOT_BE_NULL_STRING = "Destination must not be null";

    @KafkaListener(topics = "${spring.kafka.topic.friend.friend-topic}", groupId = "${spring.kafka.topic.friend.friend-group-id}")
    public void listenFriendNotifications(FriendPayload payload) {
        if (payload == null) {
            return;
        }

        String recipient = payload.recipientUsername();
        List<String> recipients = payload.recipientUsernames();
        String sender = payload.senderUsername();

        try {
            SocketResponse<NotificationMessageResponse> recipientResp = buildResponse(payload.recipientStatus(),
                    payload.senderDisplayName());
            SocketResponse<NotificationMessageResponse> senderResp = buildResponse(payload.senderStatus(),
                    payload.recipientDisplayName());

            String destination = Objects.requireNonNull(payload.destination(), DESTINATION_MUST_NOT_BE_NULL_STRING);

            if (recipients != null && !recipients.isEmpty() && recipientResp != null) {
                for (String r : recipients) {
                    routeMessage(r, destination, recipientResp);
                }
            } else if (recipient != null && recipientResp != null) {
                routeMessage(recipient, destination, recipientResp);
            }

            if (sender != null && senderResp != null) {
                routeMessage(sender, destination, senderResp);
            }
        } catch (Exception e) {
            log.error("Error sending WS notification: {}", e.getMessage(), e);
            if (payload != null && payload.senderUsername() != null) {
                routeMessage(payload.senderUsername(), "/queue/errors", SocketResponse.error("System error while processing your request", null));
            }
        }
    }

    private void routeMessage(String username, String destination, Object payload) {
        if (username == null)
            return;
        try {
            String targetServerId = (String) redisTemplate.opsForValue().get("ws:routing:" + username);
            if (targetServerId != null) {
                if (ServerIdentity.SERVER_ID.equals(targetServerId)) {
                    simpMessagingTemplate.convertAndSendToUser(username, destination, payload);
                    log.info("Sent locally to {}", username);
                } else {
                    RedisWsMessage wsMessage = new RedisWsMessage(username, destination, payload);
                    redisTemplate.convertAndSend("channel:server:" + targetServerId,
                            objectMapper.writeValueAsString(wsMessage));
                    log.info("Routed to Server {} for user {}", targetServerId, username);
                }
            } else {
                log.info("User {} is offline, skipped routing.", username);
            }
        } catch (Exception e) {
            log.error("Error routing message for {}: {}", username, e.getMessage());
        }
    }

    private SocketResponse<NotificationMessageResponse> buildResponse(NotificationsType status,
            String relatedUsername) {
        if (status == null) {
            return null;
        }

        String translationKey;
        switch (status) {
            case FRIEND_REQUEST:
                translationKey = "sys.msg.new_friend_invite";
                break;
            case REQUEST_SENT_SUCCESS:
                translationKey = "success.friend.invite_sent";
                break;
            case FRIEND_ACCEPTED:
                translationKey = "success.friend.accepted";
                break;
            case YOU_ACCEPTED:
                translationKey = "success.friend.you_accepted";
                break;
            case UNFRIENDED:
                translationKey = "success.friend.unfriended";
                break;
            case REQUEST_CANCELLED:
                translationKey = "success.friend.invite_retracted";
                break;
            case REQUEST_REJECTED:
                translationKey = "success.friend.invite_declined";
                break;
            case USER_ONLINE:
                translationKey = "sys.msg.user_online";
                break;
            case USER_OFFLINE:
                translationKey = "sys.msg.user_offline";
                break;
            default:
                translationKey = "";
        }

        NotificationMessageResponse data = NotificationMessageResponse.builder()
                .status(status)
                .relatedUsername(relatedUsername)
                .build();

        return SocketResponse.notifications(Translator.tolocale(translationKey), data);
    }
}
