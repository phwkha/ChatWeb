package com.web.backend.listener;

import com.web.backend.config.ServerIdentity;

import java.security.Principal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.web.backend.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "WEBSOCKET-LISTENER")
public class WebSocketListener {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String ONLINE_USERS_KEY = "online_users";

    private static final String ONLINE_USERS_COUNT_KEY = "online_users_count";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = headerAccessor.getUser();
        String username = (user != null) ? user.getName() : null;

        if (username != null) {

            Long count = redisTemplate.opsForHash().increment(ONLINE_USERS_COUNT_KEY, username, 1);

            if (count != null && count <= 0) {
                redisTemplate.opsForHash().put(ONLINE_USERS_COUNT_KEY, username, 1L);
                count = 1L;
            }

            redisTemplate.opsForZSet().add(ONLINE_USERS_KEY, username, System.currentTimeMillis());

            if (count != null && count == 1) {
                userService.setUserOnlineStatus(username, true);

                log.info("User Online (First Session): {}", username);
            } else {
                log.debug("User opened new tab/device: {}, total sessions: {}", username, count);
            }

            redisTemplate.opsForValue().set("ws:routing:" + username, ServerIdentity.SERVER_ID);
            log.info("Mapped User {} to Server {}", username, ServerIdentity.SERVER_ID);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = headerAccessor.getUser();
        String username = (user != null) ? user.getName() : null;

        if (username != null) {

            log.info("WebSocket Disconnected: {}", username);

            Long count = redisTemplate.opsForHash().increment(ONLINE_USERS_COUNT_KEY, username, -1);

            if (count != null && count < 0) {
                redisTemplate.opsForHash().put(ONLINE_USERS_COUNT_KEY, username, 0L);
                count = 0L;
            }

            if (count != null && count <= 0) {
                log.info("User count <= 0, scheduling offline debounce for: {}", username);
                scheduler.schedule(() -> {
                    try {
                        Object currentCountObj = redisTemplate.opsForHash().get(ONLINE_USERS_COUNT_KEY, username);
                        long currentCount = 0;
                        if (currentCountObj != null) {
                            if (currentCountObj instanceof Number number) {
                                currentCount = (number).longValue();
                            } else {
                                currentCount = Long.parseLong(currentCountObj.toString());
                            }
                        }

                        if (currentCount <= 0) {
                            redisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, username);
                            redisTemplate.opsForHash().delete(ONLINE_USERS_COUNT_KEY, username);
                            redisTemplate.delete("ws:routing:" + username);
                            userService.setUserOnlineStatus(username, false);
                            log.info("User Disconnected Completely (All sessions closed): {}", username);
                        } else {
                            log.info("User reconnected during debounce period: {}", username);
                        }
                    } catch (Exception e) {
                        log.error("Error during offline debounce", e);
                    }
                }, 5, TimeUnit.SECONDS);
            } else {
                log.info("User closed one session, still online on other devices: {}, remaining: {}", username, count);
            }
        }
    }
}