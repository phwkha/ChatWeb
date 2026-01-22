package com.web.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CUCKOO-FILTER")
public class CuckooFilterService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Thêm item vào Cuckoo Filter dùng Lua script
     */
    public void add(String key, String item) {
        String script = "return redis.call('CF.ADD', KEYS[1], ARGV[1])";
        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                RedisSerializer.string(), // Serializer cho arguments (item)
                null,                     // Serializer cho result (để null để nhận về Long)
                Collections.singletonList(key),
                item
        );
    }

    /**
     * Kiểm tra tồn tại trong Cuckoo Filter
     */
    public boolean exists(String key, String item) {
        String script = "return redis.call('CF.EXISTS', KEYS[1], ARGV[1])";
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                RedisSerializer.string(),
                null,
                Collections.singletonList(key),
                item
        );
        return result != null && result == 1L;
    }

    /**
     * Xóa item khỏi Cuckoo Filter
     */
    public void delete(String key, String item) {
        String script = "return redis.call('CF.DEL', KEYS[1], ARGV[1])";
        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                RedisSerializer.string(),
                null,
                Collections.singletonList(key),
                item
        );
        log.info("Deleted item '{}' from filter '{}'", item, key);
    }
}