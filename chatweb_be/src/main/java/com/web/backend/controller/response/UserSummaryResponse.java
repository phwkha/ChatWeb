package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryResponse {
    private String username;
    private String firstName;
    private String lastName;
    private String avatar;
    private boolean isOnline;
}
