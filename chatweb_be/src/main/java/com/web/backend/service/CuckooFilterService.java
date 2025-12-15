package com.web.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CUCKOO-FILTER")
public class CuckooFilterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void add(String key, String item) {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            return connection.execute("CF.ADD",
                    key.getBytes(StandardCharsets.UTF_8),
                    item.getBytes(StandardCharsets.UTF_8));
        });
    }

    public boolean exists(String key, String item) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            Object result = connection.execute("CF.EXISTS",
                    key.getBytes(StandardCharsets.UTF_8),
                    item.getBytes(StandardCharsets.UTF_8));

            if (result instanceof Long) {
                return ((Long) result) == 1L;
            }
            return false;
        }));
    }

    public void delete(String key, String item) {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            return connection.execute("CF.DEL",
                    key.getBytes(StandardCharsets.UTF_8),
                    item.getBytes(StandardCharsets.UTF_8));
        });
        log.info("Deleted item '{}' from filter '{}'", item, key);
    }
}