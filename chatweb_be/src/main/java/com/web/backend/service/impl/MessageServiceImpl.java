package com.web.backend.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.web.backend.common.ContentType;
import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.common.UserStatus;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import com.web.backend.repository.MessageRepository;
import com.web.backend.repository.SystemMessageRepository;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.projection.UnreadCountProjection;
import com.web.backend.service.FriendService;
import com.web.backend.service.MessageService;
import com.web.backend.exception.WebSocketErrorHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "MESSAGE-SERVICE")
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    private final UserRepository userRepository;

    private final SystemMessageRepository systemMessageRepository;

    private final FriendService friendService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final MessageMapper messageMapper;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final WebSocketErrorHandler webSocketErrorHandler;

    @Value("${spring.kafka.topic.chat.messages}")
    private String chatTopic;

    @Value("${spring.kafka.topic.chat.system-messages}")
    private String systemTopic;

    private static final long REDIS_TTL_MINUTES = 5;

    private String generateConversationId(String user1, String user2) {
        return (user1.compareTo(user2) < 0) ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    @Override
    public void sendPrivateMessage(String sender, ChatMessageRequest request) {

        UserEntity recipientEntity = userRepository.findByUsername(request.getRecipient())
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại"));

        if (recipientEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new AccessForbiddenException("Không thể nhắn tin, tài khoản này đã bị xóa.");
        }
        if (recipientEntity.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException("Không thể nhắn tin, tài khoản này đang bị tạm khóa.");
        }

        if (!friendService.isFriend(Objects.requireNonNull(sender),
                Objects.requireNonNull(request.getRecipient()))) {
            throw new AccessForbiddenException("Hai người chưa kết bạn, không thể nhắn tin.");
        }

        ChatMessage chatMessage = messageMapper.toEntity(request);
        String convId = generateConversationId(sender, request.getRecipient());
        chatMessage.setConversationId(convId);
        chatMessage.setSender(sender);
        chatMessage.setId(null);
        chatMessage.setStatus(MessageStatus.SENT);
        chatMessage.setLocalId(request.getLocalId());
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        if (chatMessage.getContent() == null) {
            chatMessage.setContent("");
        }
        if (chatMessage.getContentType() == null)
            chatMessage.setContentType(ContentType.TEXT);

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), chatMessage).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Lỗi nghiêm trọng: Không thể đẩy message lên Kafka. Topic: {}", chatTopic, ex);
                webSocketErrorHandler.handleChatError(sender, request,
                        "Hệ thống tin nhắn đang quá tải, không thể gửi tin.");
            } else {
                log.debug("Message: Push Kafka thành công offset: {}", result.getRecordMetadata().offset());
            }
        });
        if (chatMessage.getMessageType() != MessageType.CHAT) {
            return;
        }

        log.info("Đã đẩy tin nhắn từ {} lên hàng chờ lưu DB", chatMessage.getSender());

        try {
            String redisKey = "chat:recent:" + convId;
            redisTemplate.opsForList().rightPush(redisKey, chatMessage);
            redisTemplate.opsForList().trim(redisKey, -50, -1);
            redisTemplate.expire(redisKey, Objects.requireNonNull(Duration.ofMinutes(REDIS_TTL_MINUTES)));

            String key = "unread_counts:" + chatMessage.getRecipient();
            redisTemplate.opsForHash().increment(key, Objects.requireNonNull(chatMessage.getSender()), 1);
        } catch (Exception e) {
            log.warn("Redis đang gặp sự cố, bỏ qua lưu cache tạm thời: {}", e.getMessage());
        }
        log.info("save message success");
    }

    @Override
    public void sendSystemMessage(String currentUsername, MessageSystemRequest request) {
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setSender(currentUsername);
        systemMessage.setTimestamp(Instant.now());
        systemMessage.setExpiresAt(request.getSurvivalTime() == null ? null
                : Instant.now().plus(request.getSurvivalTime(), ChronoUnit.SECONDS));
        systemMessage.setContent(request.getContent());
        systemMessageRepository.save(Objects.requireNonNull(systemMessage));
        MessageSystemResponse messageSystemResponse = messageMapper.systemMessageToResponse(systemMessage);
        kafkaTemplate.send(Objects.requireNonNull(systemTopic), messageSystemResponse).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Lỗi nghiêm trọng: Không thể đẩy system message lên Kafka. Topic: {}", chatTopic, ex);
                webSocketErrorHandler.handleChatError(currentUsername, request,
                        "Hệ thống tin nhắn đang quá tải, không thể gửi tin.");
            } else {
                log.debug("System message: Push Kafka thành công offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("{} Chat system message success", systemMessage.getSender());
    }

    @Override
    public CursorResponse<ChatMessageResponse> findPrivateMessageWithCursor(String user1, String user2,
            String cursorStr, int size) {

        String conversationId = generateConversationId(user1, user2);

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ChatMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = messageRepository.findByConversationId(conversationId, pageable);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursorStr);
            messages = messageRepository.findByConversationIdAndTimestampBefore(conversationId, cursorTime, pageable);
        }

        List<ChatMessage> finalMessages = new ArrayList<>(messages);

        if (cursorStr == null || cursorStr.isEmpty()) {
            String redisKey = "chat:recent:" + conversationId;
            List<Object> redisObjects = redisTemplate.opsForList().range(redisKey, 0, -1);

            if (redisObjects != null && !redisObjects.isEmpty()) {
                Map<String, ChatMessage> uniqueMessagesMap = new LinkedHashMap<>();

                for (Object obj : redisObjects) {
                    ChatMessage msg = (ChatMessage) obj;
                    uniqueMessagesMap.put(msg.getId(), msg);
                }

                for (ChatMessage msg : messages) {
                    uniqueMessagesMap.putIfAbsent(msg.getId(), msg);
                }

                finalMessages = uniqueMessagesMap.values().stream()
                        .sorted(Comparator.comparing((ChatMessage msg) -> msg.getTimestamp()).reversed())
                        .limit(size)
                        .collect(Collectors.toList());
            }
        }
        log.info("Fetching private messages");
        return buildCursorResponse(finalMessages, size);
    }

    @Override
    public CursorResponse<MessageSystemResponse> findSystemMessageWithCursor(String cursorStr, int size) {

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<SystemMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = systemMessageRepository.findInitialMessage(pageable);
        } else {
            Instant cursorTime = Instant.parse(cursorStr);
            messages = systemMessageRepository.findMessage(cursorTime, pageable);
        }

        String nextCursor = null;
        boolean hasMore = false;

        if (!messages.isEmpty()) {
            Instant lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = lastMessageTime.toString();
            hasMore = messages.size() == size;
        }

        List<MessageSystemResponse> responseList = messages.stream()
                .map(messageMapper::systemMessageToResponse)
                .toList();

        log.info("Fetching system messages");
        return new CursorResponse<>(responseList, nextCursor, hasMore);
    }

    @Override
    public UnreadCountsResponse getUnreadMessageCounts(String recipientUsername) {

        String key = "unread_counts:" + recipientUsername;

        Map<Object, Object> redisCounts = redisTemplate.opsForHash().entries(key);

        if (!redisCounts.isEmpty()) {
            Map<String, Long> result = redisCounts.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> Long.valueOf(e.getValue().toString())));
            log.info("Fetching unread counts for user (Optimized) redis");
            return UnreadCountsResponse.builder().unreadCounts(result).build();
        }

        List<UnreadCountProjection> dbResults = messageRepository.countUnreadMessagesBySender(recipientUsername);

        Map<String, Long> resultMap = new HashMap<>();
        Map<String, Object> redisMap = new HashMap<>();

        for (UnreadCountProjection r : dbResults) {
            resultMap.put(r.getSender(), r.getCount());
            redisMap.put(r.getSender(), r.getCount());
        }

        if (!redisMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, redisMap);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        }

        log.info("Fetching unread counts for user (From DB)");
        return UnreadCountsResponse.builder()
                .unreadCounts(resultMap)
                .build();
    }

    @Override
    public void markMessagesAsRead(String recipientUsername, String senderUsername) {
        List<ChatMessage> messages = messageRepository.findUnreadMessagesFromSender(recipientUsername, senderUsername);
        if (!messages.isEmpty()) {
            messages.forEach(msg -> msg.setStatus(MessageStatus.READ));
            messageRepository.saveAll(messages);
        }
        String key = "unread_counts:" + recipientUsername;
        redisTemplate.opsForHash().delete(key, senderUsername);
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setMessageType(MessageType.STATUS);
        statusMsg.setSender(senderUsername);
        statusMsg.setRecipient(recipientUsername);

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), statusMsg);

        log.info("User marking messages");
    }

    private CursorResponse<ChatMessageResponse> buildCursorResponse(List<ChatMessage> messages, int size) {
        String nextCursor = null;
        boolean hasMore = false;

        if (!messages.isEmpty()) {
            LocalDateTime lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = lastMessageTime.toString();
            hasMore = messages.size() == size;
        }

        List<ChatMessageResponse> responseList = messages.stream()
                .map(messageMapper::toResponse)
                .toList();

        return new CursorResponse<>(responseList, nextCursor, hasMore);
    }
}
