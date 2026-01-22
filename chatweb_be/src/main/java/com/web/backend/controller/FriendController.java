package com.web.backend.controller;

import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.controller.response.PageResponse;
import com.web.backend.controller.response.UserSummaryResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-CONTROLLER")
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getFriendRequests(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        log.info("Lấy danh sách lời mời kết bạn cho: {}", user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách lời mời thành công",
                friendService.getPendingRequests(user.getUsername(), page, size, sortBy)
        ));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getSentRequests(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách lời mời đã gửi thành công",
                friendService.getSentRequests(user.getUsername(), page, size)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getFriendsList(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createAt") String sortBy
    ) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        log.info("Lấy danh sách bạn bè cho: {}", user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Lấy danh sách bạn bè thành công",
                friendService.getFriendsList(user.getUsername(), page, size, sortBy)
        ));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteFriendship(
            Authentication auth,
            @PathVariable String username) {

        UserEntity user = (UserEntity) auth.getPrincipal();
        friendService.deleteFriendship(user.getUsername(), username);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Thao tác thành công",
                null
        ));
    }

    @PostMapping("/block/{username}")
    public ResponseEntity<ApiResponse<Void>> blockUser(Authentication auth, @PathVariable String username) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        friendService.blockUser(user.getUsername(), username);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Đã chặn người dùng " + username,
                null));
    }
}