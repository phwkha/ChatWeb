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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.web.backend.common.ContentType;
import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import com.web.backend.common.UserStatus;
import com.web.backend.config.LocalResolverConfig.Translator;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.request.ReactionRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.exception.WebSocketErrorHandler;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.MessageRepository;
import com.web.backend.repository.SystemMessageRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.projection.UnreadCountProjection;
import com.web.backend.service.FriendService;
import com.web.backend.service.MessageService;

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

    private final MongoTemplate mongoTemplate;

    private final MessageMapper messageMapper;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final WebSocketErrorHandler webSocketErrorHandler;

    @Value("${spring.kafka.topic.chat.messages}")
    private String chatTopic;

    @Value("${spring.kafka.topic.chat.system-messages}")
    private String systemTopic;

    private static final long REDIS_TTL_MINUTES = 5;

    private static final String CONVERSATIONID_STRING = "conversationId";

    private static final String ID_STRING = "id";
    private static final String TIMESTAMP_STRING = "timestamp";

    private static final String CHAT_RECENT_STRING = "chat:recent:";
    private static final String UNREAD_COUNTS_STRING = "unread_counts:";

    private static final String REACTIONS_STRING = "reactions.";

    private static final String ERROR_MSG_RECIPIENT_NOT_FOUND_STRING = "error.msg.recipient_not_found";
    private static final String ERROR_MSG_SEND_DELETED_STRING = "error.msg.send_deleted";
    private static final String ERROR_MSG_SEND_LOCKED_STRING = "error.msg.send_locked";
    private static final String ERROR_MSG_NOT_FRIENDS_STRING = "error.msg.not_friends";
    private static final String ERROR_MSG_SYSTEM_OVERLOAD_STRING = "error.msg.system_overload";
    private static final String ERROR_MSG_NOT_FOUND_STRING = "error.msg.not_found";
    private static final String ERROR_MSG_EDIT_FORBIDDEN_STRING = "error.msg.edit_forbidden";
    private static final String ERROR_MSG_DELETE_FORBIDDEN_STRING = "error.msg.delete_forbidden";

    private String generateConversationId(String user1, String user2) {
        return (user1.compareTo(user2) < 0) ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    @Override
    public void sendPrivateMessage(String sender, ChatMessageRequest request) {

        UserEntity recipientEntity = userRepository.findByUsername(request.getRecipient())
                .orElseThrow(
                        () -> new ResourceNotFoundException(Translator.tolocale(ERROR_MSG_RECIPIENT_NOT_FOUND_STRING)));

        if (recipientEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_SEND_DELETED_STRING));
        }
        if (recipientEntity.getUserStatus() == UserStatus.LOCKED) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_SEND_LOCKED_STRING));
        }

        if (!friendService.isFriend(Objects.requireNonNull(sender),
                Objects.requireNonNull(request.getRecipient()))) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_NOT_FRIENDS_STRING));
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
                log.error("Critical Error: Cannot push message to Kafka. Topic: {}", chatTopic, ex);
                webSocketErrorHandler.handleChatError(sender, request,
                        Translator.tolocale(ERROR_MSG_SYSTEM_OVERLOAD_STRING));
            } else {
                log.debug("Message: Kafka push successful offset: {}", result.getRecordMetadata().offset());
            }
        });
        if (chatMessage.getMessageType() != MessageType.CHAT) {
            return;
        }

        log.info("Pushed message from {} to DB save queue", chatMessage.getSender());

        try {
            String redisKey = CHAT_RECENT_STRING + convId;
            redisTemplate.opsForList().rightPush(redisKey, chatMessage);
            redisTemplate.opsForList().trim(redisKey, -50, -1);
            redisTemplate.expire(redisKey, Objects.requireNonNull(Duration.ofMinutes(REDIS_TTL_MINUTES)));

            String key = UNREAD_COUNTS_STRING + chatMessage.getRecipient();
            redisTemplate.opsForHash().increment(key, Objects.requireNonNull(chatMessage.getSender()), 1);
        } catch (Exception e) {
            log.warn("Redis is encountering issues, skipping temporary cache save: {}", e.getMessage());
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
                log.error("Critical Error: Cannot push system message to Kafka. Topic: {}", chatTopic, ex);
                webSocketErrorHandler.handleChatError(currentUsername, request,
                        Translator.tolocale(ERROR_MSG_SYSTEM_OVERLOAD_STRING));
            } else {
                log.debug("System message: Kafka push successful offset: {}", result.getRecordMetadata().offset());
            }
        });
        log.info("{} Chat system message success", systemMessage.getSender());
    }

    @Override
    public void reactToMessage(String senderUsername, ReactionRequest request) {

        if (!friendService.isFriend(Objects.requireNonNull(senderUsername),
                Objects.requireNonNull(request.getRecipient()))) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_NOT_FRIENDS_STRING));
        }

        String convId = generateConversationId(senderUsername, request.getRecipient());
        Query query = new Query(
                Criteria.where(ID_STRING).is(request.getMessageId()).and(CONVERSATIONID_STRING).is(convId));
        Update update = new Update();

        String reactionField = REACTIONS_STRING + senderUsername;
        if (request.getReactionType() != null) {
            update.set(reactionField, request.getReactionType());
        } else {
            update.unset(reactionField);
        }
        mongoTemplate.updateFirst(query, update, ChatMessage.class);

        String redisKey = CHAT_RECENT_STRING + convId;
        redisTemplate.delete(redisKey);

        ChatMessage reactionMsg = new ChatMessage();
        reactionMsg.setId(request.getMessageId());
        reactionMsg.setConversationId(convId);
        reactionMsg.setSender(senderUsername);
        reactionMsg.setRecipient(request.getRecipient());
        reactionMsg.setMessageType(MessageType.REACTION);
        reactionMsg.setContent(request.getReactionType() != null ? request.getReactionType().toString() : "");
        reactionMsg.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), reactionMsg).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Reaction: Failed to push to Kafka. Topic: {}", chatTopic, ex);
            } else {
                log.debug("Reaction: Successfully pushed to Kafka");
            }
        });
    }

    @Override
    public CursorResponse<ChatMessageResponse> findPrivateMessageWithCursor(String user1, String user2,
            String cursorStr, int size) {

        String conversationId = generateConversationId(user1, user2);

        Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, TIMESTAMP_STRING));
        List<ChatMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = messageRepository.findByConversationId(conversationId, pageable);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursorStr);
            messages = messageRepository.findByConversationIdAndTimestampBefore(conversationId, cursorTime, pageable);
        }

        List<ChatMessage> finalMessages = new ArrayList<>(messages);

        if (cursorStr == null || cursorStr.isEmpty()) {
            String redisKey = CHAT_RECENT_STRING + conversationId;
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
                        .limit(size + 1)
                        .collect(Collectors.toList());
            }
        }
        log.info("Fetching private messages");
        return buildCursorResponse(finalMessages, size);
    }

    @Override
    public ChatMessageResponse getMessageById(String messageId, String currentUsername) {
        ChatMessage message = messageRepository.findById(Objects.requireNonNull(messageId))
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale(ERROR_MSG_NOT_FOUND_STRING)));

        if (!message.getConversationId().contains(currentUsername)) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_RECIPIENT_NOT_FOUND_STRING));
        }

        return messageMapper.toResponse(message);
    }

    @Override
    public void editMessage(String senderUsername, com.web.backend.controller.request.EditMessageRequest request) {
        ChatMessage message = messageRepository.findById(Objects.requireNonNull(request.getMessageId()))
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale(ERROR_MSG_NOT_FOUND_STRING)));

        if (!message.getSender().equals(senderUsername)) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_EDIT_FORBIDDEN_STRING));
        }

        message.setContent(request.getNewContent());
        message.setEdited(true);

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), message).whenComplete((result, ex) -> {
            if (ex != null)
                log.error("Failed to push edit to Kafka", ex);
        });
    }

    @Override
    public void revokeMessage(String senderUsername, com.web.backend.controller.request.RevokeMessageRequest request) {
        ChatMessage message = messageRepository.findById(Objects.requireNonNull(request.getMessageId()))
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale(ERROR_MSG_NOT_FOUND_STRING)));

        if (!message.getSender().equals(senderUsername)) {
            throw new AccessForbiddenException(Translator.tolocale(ERROR_MSG_DELETE_FORBIDDEN_STRING));
        }

        message.setContent("");
        message.setFileUrl(null);
        message.setFileName(null);
        message.setFileSize(null);
        message.setReactions(null);
        message.setDeleted(true);

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), message).whenComplete((result, ex) -> {
            if (ex != null)
                log.error("Failed to push revoke to Kafka", ex);
        });
    }

    @Override
    public CursorResponse<MessageSystemResponse> findSystemMessageWithCursor(String cursorStr, int size) {

        Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, TIMESTAMP_STRING));
        List<SystemMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = new ArrayList<>(systemMessageRepository.findInitialMessage(pageable));
        } else {
            Instant cursorTime = Instant.parse(cursorStr);
            messages = new ArrayList<>(systemMessageRepository.findMessage(cursorTime, pageable));
        }

        boolean hasMore = false;
        if (messages.size() > size) {
            hasMore = true;
            messages.remove(messages.size() - 1);
        }

        String nextCursor = null;
        if (!messages.isEmpty()) {
            Instant lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = lastMessageTime.toString();
        }

        List<MessageSystemResponse> responseList = messages.stream()
                .map(messageMapper::systemMessageToResponse)
                .toList();

        log.info("Fetching system messages");
        return new CursorResponse<>(responseList, nextCursor, hasMore);
    }

    @Override
    public UnreadCountsResponse getUnreadMessageCounts(String recipientUsername) {

        String key = UNREAD_COUNTS_STRING + recipientUsername;

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
        String key = UNREAD_COUNTS_STRING + recipientUsername;
        redisTemplate.opsForHash().delete(key, senderUsername);
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setMessageType(MessageType.STATUS);
        statusMsg.setStatus(MessageStatus.READ);
        statusMsg.setSender(senderUsername);
        statusMsg.setRecipient(recipientUsername);

        kafkaTemplate.send(Objects.requireNonNull(chatTopic), statusMsg);

        log.info("User marking messages");
    }

    private CursorResponse<ChatMessageResponse> buildCursorResponse(List<ChatMessage> messages, int size) {

        boolean hasMore = false;
        if (messages.size() > size) {
            hasMore = true;
            messages.remove(messages.size() - 1);
        }

        String nextCursor = null;
        if (!messages.isEmpty()) {
            LocalDateTime lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = lastMessageTime.toString();
        }

        List<ChatMessageResponse> responseList = messages.stream()
                .map(messageMapper::toResponse)
                .toList();

        return new CursorResponse<>(responseList, nextCursor, hasMore);
    }
}
