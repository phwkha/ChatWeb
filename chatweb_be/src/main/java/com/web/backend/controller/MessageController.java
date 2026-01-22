package com.web.backend.controller;

import com.web.backend.controller.request.MarkReadRequest;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.UnreadCountsResponse;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j(topic = "MESSAGE-CONTROLLER")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/private")
    public ResponseEntity<ApiResponse<CursorResponse<ChatMessage>>> getPrivateMessage(
            @RequestParam String user1,
            @RequestParam String user2,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching private messages between {} and {}", user1, user2);

        CursorResponse<ChatMessage> response = messageService.findPrivateMessageWithCursor(user1, user2, cursor, size);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Get private messages success", response));
    }

    @GetMapping("/group")
    public ResponseEntity<ApiResponse<CursorResponse<ChatMessage>>> getGroupMessage(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching group messages with cursor: {}", cursor);

        CursorResponse<ChatMessage> response = messageService.findMessageByMessageTypeIsChat(cursor, size);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Get group messages success", response));
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<ApiResponse<UnreadCountsResponse>> getUnreadCounts(Authentication auth) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        log.info("Fetching unread counts for user: {}", user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Get unread counts success",messageService.getUnreadMessageCounts(user.getUsername())));
    }

    @PostMapping("/mark-as-read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication auth,
            @RequestBody @Valid MarkReadRequest request) {

        UserEntity user = (UserEntity) auth.getPrincipal();

        log.info("User {} marking messages from {} as read", user.getUsername(), request.getSender());

        messageService.markMessagesAsRead(user.getUsername(), request.getSender());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Messages marked as read", null));
    }
}