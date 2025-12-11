package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OnlineUsersResponse {
    Map<String, UserSummaryResponse> users;
}
