package com.web.backend.service.impl;

import com.web.backend.common.MessageStatus;
import com.web.backend.common.MessageType;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.UnreadCountResultResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.event.NewChatMessageEvent;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.repository.MessageRepository;
import com.web.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j(topic = "MESSAGE-SERVICE")
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private final MessageMapper messageMapper;

    private final ApplicationEventPublisher eventPublisher;

    private String generateConversationId(String user1, String user2) {
        return (user1.compareTo(user2) < 0) ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {

        if (chatMessage.getMessageType() == MessageType.PRIVATE_CHAT) {
            String convId = generateConversationId(chatMessage.getSender(), chatMessage.getRecipient());
            chatMessage.setConversationId(convId);
        }

        ChatMessage savedMessage = messageRepository.save(chatMessage);

        String key = "unread_counts:" + chatMessage.getRecipient();
        redisTemplate.opsForHash().increment(key, chatMessage.getSender(), 1);

        ChatMessageResponse response = messageMapper.toResponse(savedMessage);
        response.setLocalId(chatMessage.getLocalId());

        eventPublisher.publishEvent(new NewChatMessageEvent(
                this,
                response,
                savedMessage.getSender(),
                savedMessage.getRecipient()
        ));

        return savedMessage;
    }

    @Override
    public CursorResponse<ChatMessage> findPrivateMessageWithCursor(String user1, String user2, String cursorStr, int size) {

        String conversationId = generateConversationId(user1, user2);

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ChatMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = messageRepository.findByConversationId(conversationId, pageable);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursorStr);
            messages = messageRepository.findByConversationIdAndTimestampBefore(conversationId, cursorTime, pageable);
        }
        log.info("Fetching private messages");
        return buildCursorResponse(messages, size);
    }

    @Override
    public CursorResponse<ChatMessage> findMessageByMessageTypeIsChat(String cursorStr, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ChatMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = messageRepository.findInitialMessageByMessageTypeIsChat(pageable);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursorStr);
            messages = messageRepository.findMessageByMessageTypeIsChat(cursorTime, pageable);
        }
        log.info("Fetching group messages");
        return buildCursorResponse(messages, size);
    }

    @Override
    public UnreadCountsResponse getUnreadMessageCounts(String recipientUsername) {

        String key = "unread_counts:" + recipientUsername;
        Map<Object, Object> redisCounts = redisTemplate.opsForHash().entries(key);

        if (!redisCounts.isEmpty()) {
            Map<String, Long> result = redisCounts.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> Long.valueOf(e.getValue().toString())
                    ));
            log.info("Fetching unread counts for user (Optimized) redis");
            return UnreadCountsResponse.builder().unreadCounts(result).build();
        }

        List<UnreadCountResultResponse> dbResults = messageRepository.countUnreadMessagesBySender(recipientUsername);

        Map<String, Long> countMap = new HashMap<>();
        for (UnreadCountResultResponse r : dbResults) {
            countMap.put(r.getSenderId(), r.getCount());
            redisTemplate.opsForHash().put(key, r.getSenderId(), r.getCount());
        }
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
        log.info("Fetching unread counts for user (Optimized)");
        return UnreadCountsResponse.builder()
                .unreadCounts(countMap)
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
        log.info("User marking messages");
    }

    @Override
    public boolean hasMessages(String username) {
        return messageRepository.existsBySenderOrRecipient(username);
    }

    private CursorResponse<ChatMessage> buildCursorResponse(List<ChatMessage> messages, int size) {
        String nextCursor = null;
        boolean hasMore = false;

        if (!messages.isEmpty()) {
            LocalDateTime lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = lastMessageTime.toString();
            hasMore = messages.size() == size;
        }

        return new CursorResponse<>(messages, nextCursor, hasMore);
    }
}
