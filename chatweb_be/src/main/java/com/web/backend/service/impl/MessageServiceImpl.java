package com.web.backend.service.impl;

import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.model.ChatMessage;
import com.web.backend.repository.MessageRepository;
import com.web.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j(topic = "MESSAGE-SERVICE")
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        return messageRepository.save(chatMessage);
    }

    @Override
    public CursorResponse<ChatMessage> findPrivateMessageWithCursor(String user1, String user2, String cursorStr, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ChatMessage> messages;

        if (cursorStr == null || cursorStr.isEmpty()) {
            messages = messageRepository.findInitialMessages(user1, user2, pageable);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursorStr);
            messages = messageRepository.findMessagesBeforeCursor(user1, user2, cursorTime, pageable);
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
        List<ChatMessage> unread = messageRepository.findUnreadPrivateMessages(recipientUsername);
        log.info("Fetching unread counts for user");
        return UnreadCountsResponse.builder()
                .unreadCounts(unread.stream().collect(Collectors.groupingBy(ChatMessage::getSender, Collectors.counting())))
                .build();
    }

    @Override
    public void markMessagesAsRead(String recipientUsername, String senderUsername) {
        List<ChatMessage> messages = messageRepository.findUnreadMessagesFromSender(recipientUsername, senderUsername);
        if (messages.isEmpty()) {
            return;
        }
        messages.forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(messages);
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
