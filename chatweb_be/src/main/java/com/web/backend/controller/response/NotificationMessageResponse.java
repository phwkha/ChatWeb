package com.web.backend.controller.response;

import com.web.backend.common.NotificationsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageResponse {

    private NotificationsStatus status;

    private String relatedUsername;

}
