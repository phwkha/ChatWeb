package com.web.backend.kafka.payload;

import com.web.backend.controller.response.NotificationMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendNotificationMessage {
    private String senderUsername;
    private String recipientUsername;
    private String destination;
    private SocketResponse<NotificationMessageResponse> senderResponse;
    private SocketResponse<NotificationMessageResponse> recipientResponse;
}
