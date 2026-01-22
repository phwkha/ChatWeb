package com.web.backend.JWT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.backend.common.TokenType;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.JwtService;
import com.web.backend.service.util.UserServiceDetail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "JWT-AUTHENTICATION-FLITER")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserServiceDetail userServiceDetail;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getTokenFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                String key = "blacklist:" + jwt;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    ApiResponse<?> apiResponse = ApiResponse.error(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Token đã đăng xuất (Blacklisted)"
                    );

                    ObjectMapper objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

                    response.flushBuffer();
                    return;
                }

                String username = jwtService.extractUsername(jwt, TokenType.ACCESS_TOKEN);

                if (username != null) {

                    UserDetails userDetails = userServiceDetail.loadUserByUsername(username);

                    if (userDetails instanceof UserEntity userEntity) {
                        Integer tokenVersionInJwt = jwtService.extractClaim(jwt,TokenType.ACCESS_TOKEN ,claims -> claims.get("v", Integer.class));

                        Integer currentVersion = userEntity.getTokenVersion();
                        if (currentVersion == null) currentVersion = 0;

                        if (tokenVersionInJwt == null || !tokenVersionInJwt.equals(currentVersion)) {
                            log.warn("Token version mismatch for user {}. Token: {}, Server: {}", username, tokenVersionInJwt, currentVersion);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }

                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    securityContext.setAuthentication(authToken);
                    SecurityContextHolder.setContext(securityContext);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Hàm hỗ trợ lấy Token từ Cookie (ưu tiên) hoặc Header Authorization
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}