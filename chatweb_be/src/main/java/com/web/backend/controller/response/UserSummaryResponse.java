package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;
import com.web.backend.common.UserStatus;

@Data
@Builder
public class UserSummaryResponse {
    private String username;
    private String firstName;
    private String lastName;
    private String avatar;
    private boolean isOnline;
    private UserStatus userStatus;
}
