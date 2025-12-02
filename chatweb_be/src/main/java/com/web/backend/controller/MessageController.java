package com.web.backend.controller;

import com.web.backend.controller.response.CursorResponse;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/private")
    public ResponseEntity<CursorResponse<ChatMessage>> getPrivateMessage(
            @RequestParam String user1,
            @RequestParam String user2,
            @RequestParam(required = false) String cursor, // Cursor có thể null (lần đầu)
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorResponse<ChatMessage> response = messageService.findPrivateMessageWithCursor(user1, user2, cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("group")
    public ResponseEntity<CursorResponse<ChatMessage>> getGroupMessage(
            @RequestParam(required = false) String cursor, // Cursor có thể null (lần đầu)
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorResponse<ChatMessage> response = messageService.findMessageByMessageTypeIsChat(cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<Map<String, Long>> getUnreadCounts(Authentication auth) {
        // Lấy username của người đang đăng nhập
        String username = ((UserEntity) auth.getPrincipal()).getUsername();
        return ResponseEntity.ok(messageService.getUnreadMessageCounts(username));
    }

    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(Authentication auth, @RequestBody Map<String, String> payload) {
        String username = ((UserEntity) auth.getPrincipal()).getUsername(); // Đây là người nhận (B)
        String senderUsername = payload.get("sender"); // Đây là người gửi (A)

        if (senderUsername != null) {
            messageService.markMessagesAsRead(username, senderUsername);
        }
        return ResponseEntity.ok().build();
    }
}
