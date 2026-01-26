package com.web.backend.service;

import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;

import java.util.List;
import java.util.Map;

public interface MessageService {
    void saveMessage(ChatMessage chatMessage);

    void saveSystemMessage(SystemMessage systemMessage);

    void messageTyping(ChatMessage chatMessage);

    CursorResponse<ChatMessage> findPrivateMessageWithCursor(String user1, String user2, String cursorStr, int size);

    UnreadCountsResponse getUnreadMessageCounts(String recipientUsername);

    void markMessagesAsRead(String recipientUsername, String senderUsername);

    boolean hasMessages(String username);

}
