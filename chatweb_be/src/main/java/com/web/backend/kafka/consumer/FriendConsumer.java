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

    private static final String WS_ROUTING_STRING = "ws:routing:";
    private static final String CHANNEL_SERVER_STRING = "channel:server:";

    private static final String ERROR_SYS_PROCESSING_REQ_STRING = "error.sys.processing_req";
    private static final String QUEUE_ERRORS_STRING = "/queue/errors";

    private static final String SYS_MSG_NEW_FRIEND_INVITE_STRING = "sys.msg.new_friend_invite";
    private static final String SUCCESS_FRIEND_INVITE_SENT_STRING = "success.friend.invite_sent";
    private static final String SUCCESS_FRIEND_ACCEPTED_STRING = "success.friend.accepted";
    private static final String SUCCESS_FRIEND_YOU_ACCEPTED_STRING = "success.friend.you_accepted";
    private static final String SUCCESS_FRIEND_UNFRIENDED_STRING = "success.friend.unfriended";
    private static final String SUCCESS_FRIEND_INVITE_RETRACTED_STRING = "success.friend.invite_retracted";
    private static final String SUCCESS_FRIEND_INVITE_DECLINED_STRING = "success.friend.invite_declined";
    private static final String SYS_MSG_USER_ONLINE_STRING = "sys.msg.user_online";
    private static final String SYS_MSG_USER_OFFLINE_STRING = "sys.msg.user_offline";

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
                try {
                    routeMessage(payload.senderUsername(), QUEUE_ERRORS_STRING, SocketResponse.error(
                            Translator.tolocale(ERROR_SYS_PROCESSING_REQ_STRING), payload));
                } catch (Exception ex) {
                    log.error("Failed to send error notification to sender: {}", ex.getMessage());
                }
            }
        }
    }

    private void routeMessage(String username, String destination, Object payload) throws Exception {
        if (username == null)
            return;
        String targetServerId = (String) redisTemplate.opsForValue().get(WS_ROUTING_STRING + username);
        if (targetServerId != null) {
            if (ServerIdentity.SERVER_ID.equals(targetServerId)) {
                simpMessagingTemplate.convertAndSendToUser(username, destination, payload);
                log.info("Sent locally to {}", username);
            } else {
                RedisWsMessage wsMessage = new RedisWsMessage(username, destination, payload);
                redisTemplate.convertAndSend(CHANNEL_SERVER_STRING + targetServerId,
                        objectMapper.writeValueAsString(wsMessage));
                log.info("Routed to Server {} for user {}", targetServerId, username);
            }
        } else {
            log.info("User {} is offline, skipped routing.", username);
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
                translationKey = SYS_MSG_NEW_FRIEND_INVITE_STRING;
                break;
            case REQUEST_SENT_SUCCESS:
                translationKey = SUCCESS_FRIEND_INVITE_SENT_STRING;
                break;
            case FRIEND_ACCEPTED:
                translationKey = SUCCESS_FRIEND_ACCEPTED_STRING;
                break;
            case YOU_ACCEPTED:
                translationKey = SUCCESS_FRIEND_YOU_ACCEPTED_STRING;
                break;
            case UNFRIENDED:
                translationKey = SUCCESS_FRIEND_UNFRIENDED_STRING;
                break;
            case REQUEST_CANCELLED:
                translationKey = SUCCESS_FRIEND_INVITE_RETRACTED_STRING;
                break;
            case REQUEST_REJECTED:
                translationKey = SUCCESS_FRIEND_INVITE_DECLINED_STRING;
                break;
            case USER_ONLINE:
                translationKey = SYS_MSG_USER_ONLINE_STRING;
                break;
            case USER_OFFLINE:
                translationKey = SYS_MSG_USER_OFFLINE_STRING;
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
