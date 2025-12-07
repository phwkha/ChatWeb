package com.web.backend.controller.response;

import com.web.backend.model.DTO.UserDTO;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OnlineUsersResponse {
    Map<String, UserDTO> users;
}
