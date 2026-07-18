package com.web.backend.controller.websocket;

import com.web.backend.exception.WebSocketErrorHandler;
import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FriendWebSocketControllerTest {

    @Mock
    private FriendService friendService;

    @Mock
    private WebSocketErrorHandler webSocketErrorHandler;

    @InjectMocks
    private FriendWebSocketController friendWebSocketController;

    private UsernamePasswordAuthenticationToken mockAuth;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setUsername("testuser");
        mockAuth = new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
    }

    @Test
    void testHandleFriendRequest_Success() {
        friendWebSocketController.handleFriendRequest("targetuser", mockAuth);

        verify(friendService).sendFriendRequest("testuser", "targetuser");
    }

    @Test
    void testHandleAcceptRequest_Success() {
        friendWebSocketController.handleAcceptRequest("requesteruser", mockAuth);

        verify(friendService).acceptFriendRequest("testuser", "requesteruser");
    }
}
