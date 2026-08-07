package com.web.backend.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

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

    private final SimpUserRegistry simpUserRegistry;

    private static final String DESTINATION_MUST_NOT_BE_NULL_STRING = "Destination must not be null";

    @KafkaListener(topics = "${spring.kafka.topic.friend}", groupId = "${spring.kafka.topic.friend-group-id}-${random.uuid}")
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

            if (recipients != null && !recipients.isEmpty() && recipientResp != null) {
                for (String r : recipients) {
                    if (simpUserRegistry.getUser(r) != null) {
                        simpMessagingTemplate.convertAndSendToUser(
                                r,
                                Objects.requireNonNull(payload.destination(), DESTINATION_MUST_NOT_BE_NULL_STRING),
                                recipientResp);
                    }
                }
            } else if (recipient != null && recipientResp != null
                    && simpUserRegistry.getUser(recipient) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        recipient,
                        Objects.requireNonNull(payload.destination(), DESTINATION_MUST_NOT_BE_NULL_STRING),
                        recipientResp);
                log.info("Sent friend notification via WS to recipient: {}", recipient);
            }

            if (sender != null && senderResp != null && simpUserRegistry.getUser(sender) != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        sender,
                        Objects.requireNonNull(payload.destination(), DESTINATION_MUST_NOT_BE_NULL_STRING),
                        senderResp);
                log.info("Sent friend notification via WS to sender: {}", sender);
            }
        } catch (Exception e) {
            log.error("Error sending WS notification: {}", e.getMessage(), e);
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
