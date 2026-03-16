package com.web.backend.service;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import com.web.backend.repository.SystemMessageRepository;

import java.util.List;
import java.util.Map;

public interface MessageService {
    void saveMessage(ChatMessage chatMessage);

    void saveSystemMessage(SystemMessage systemMessage);

    void messageTyping(ChatMessage chatMessage);

    CursorResponse<ChatMessageResponse> findPrivateMessageWithCursor(String user1, String user2, String cursorStr, int size);

    CursorResponse<MessageSystemResponse> findSystemMessageWithCursor(String cursorStr, int size);

    UnreadCountsResponse getUnreadMessageCounts(String recipientUsername);

    void markMessagesAsRead(String recipientUsername, String senderUsername);

    boolean hasMessages(String username);

}
