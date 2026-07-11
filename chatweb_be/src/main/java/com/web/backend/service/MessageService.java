package com.web.backend.service;

import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.MessageSystemRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.UnreadCountsResponse;

public interface MessageService {
    void sendPrivateMessage(String sender, ChatMessageRequest request);

    void sendSystemMessage(String currentUsername, MessageSystemRequest request);

    CursorResponse<ChatMessageResponse> findPrivateMessageWithCursor(String user1, String user2, String cursorStr,
            int size);

    CursorResponse<MessageSystemResponse> findSystemMessageWithCursor(String cursorStr, int size);

    UnreadCountsResponse getUnreadMessageCounts(String recipientUsername);

    void markMessagesAsRead(String recipientUsername, String senderUsername);
}
