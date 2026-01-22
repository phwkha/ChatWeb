package com.web.backend.service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean allowRequest(String ipAddress, String action, int maxRequests, int timeWindowSeconds) {

        String key = "rate_limit:" + action + ":" + ipAddress;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
        }

        return currentCount != null && currentCount <= maxRequests;
    }
}