package com.web.backend.config;

import com.web.backend.JWT.JwtHandshakeInterceptor;
import com.web.backend.common.TokenType;
import com.web.backend.model.UserEntity;
import com.web.backend.service.JwtService;
import com.web.backend.service.UserServiceDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j(topic = "WEBSOCKET-CONFIG")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserServiceDetail userServiceDetail;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5174", "http://localhost:8080", "http://localhost:5173")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String token = (String) accessor.getSessionAttributes().get("jwt_token_cookie");

                    if (token == null) {
                        token = extractTokenFromHeader(accessor);
                    }

                    if (token != null) {
                        try {
                            String key = "blacklist:" + token;
                            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                                log.info("Token hết hạn");
                                throw new MessagingException("Token đã đăng xuất (Blacklisted)");
                            }

                            String username = jwtService.extractUsername(token, TokenType.ACCESS_TOKEN);

                            if (username != null) {
                                UserDetails userDetails = userServiceDetail.loadUserByUsername(username);

                                if (userDetails instanceof UserEntity userEntity) {
                                    Integer tokenVersionInJwt = jwtService.extractClaim(token, TokenType.ACCESS_TOKEN, claims -> claims.get("v", Integer.class));

                                    Integer currentVersion = userEntity.getTokenVersion();
                                    if (currentVersion == null) currentVersion = 0;

                                    if (tokenVersionInJwt == null || !tokenVersionInJwt.equals(currentVersion)) {
                                        log.warn("Token version mismatch for user in WebSocket: {}", username);
                                        throw new MessagingException("Token không hợp lệ (Phiên bản cũ)");
                                    }
                                }

                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                log.info("WebSocket Authenticated User: {}", username);
                            }
                        } catch (Exception e) {
                            log.error("WebSocket Auth Failed: {}", e.getMessage());
                            throw new MessagingException("Lỗi xác thực: " + e.getMessage());
                        }
                    } else {
                        throw new MessagingException("Không tìm thấy Access Token (Header/Cookie)");
                    }
                }
                return message;
            }
        });
    }

    private String extractTokenFromHeader(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}
