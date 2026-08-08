package com.web.backend.kafka.consumer;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.web.backend.common.MessageType;
import com.web.backend.model.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "REDIS-CACHE-CONSUMER")
public class RedisCacheConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_RECENT_HASH_STRING = "chat:recent:hash:";
    private static final String CHAT_RECENT_ZSET_STRING = "chat:recent:zset:";
    private static final String UNREAD_COUNTS_STRING = "unread:counts:";
    private static final long REDIS_TTL_MINUTES = 5;

    @KafkaListener(topics = "${spring.kafka.topic.chat.messages}", groupId = "${spring.kafka.topic.chat.messages-group-id}-redis-cache")
    public void updateRecentChats(ChatMessage chatMsg) {
        if (chatMsg == null || chatMsg.getMessageType() != MessageType.CHAT) {
            return;
        }

        String convId = chatMsg.getConversationId();
        if (convId == null) {
            return;
        }

        log.info("Kafka Consumer: Caching message from {} to Redis", chatMsg.getSender());
        try {
            String hashKey = CHAT_RECENT_HASH_STRING + convId;
            String zsetKey = CHAT_RECENT_ZSET_STRING + convId;

            redisTemplate.opsForHash().put(hashKey, chatMsg.getId(), chatMsg);
            long score = chatMsg.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            redisTemplate.opsForZSet().add(zsetKey, chatMsg.getId(), score);

            redisTemplate.opsForZSet().removeRange(zsetKey, 0, -51);

            redisTemplate.expire(hashKey, Objects.requireNonNull(Duration.ofMinutes(REDIS_TTL_MINUTES)));
            redisTemplate.expire(zsetKey, Objects.requireNonNull(Duration.ofMinutes(REDIS_TTL_MINUTES)));

            String key = UNREAD_COUNTS_STRING + chatMsg.getRecipient();
            redisTemplate.opsForHash().increment(key, Objects.requireNonNull(chatMsg.getSender()), 1);
        } catch (Exception e) {
            log.error("Error caching message to Redis in background", e);
        }
    }
}
