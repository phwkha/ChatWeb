package com.web.backend.controller;

import com.web.backend.common.Convert;
import com.web.backend.common.Role;
import com.web.backend.controller.request.ChangePasswordRequest;
import com.web.backend.controller.request.CreateUserRequest;
import com.web.backend.controller.request.UpdateUserRequest;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        System.out.println("Get online users called");
        System.out.println("Current users: " + userService.getOnlineUsers());
        return ResponseEntity.ok(userService.getOnlineUsers());
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authorized");
        }

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();

        return ResponseEntity.ok(Convert.UserConvertToUserDTO(userEntityPrincipal));
    }

    @GetMapping("/public-key/{username}")
    public ResponseEntity<Map<String, String>> getPublicKey(@PathVariable String username) {
        String publicKey = userService.getPublicKey(username);

        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        response.put("publicKey", publicKey);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody CreateUserRequest createUserRequest) {
        UserDTO newUser = userService.addUser(createUserRequest);
        return ResponseEntity.ok(newUser);
    }

    @PostMapping("/public-key")
    public ResponseEntity<Void> savePublicKey(Authentication authentication, @RequestBody Map<String, String> payload) {

        // Lấy principal (là User object) từ Authentication
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        // Lấy username thật từ object đó
        String username = userEntityPrincipal.getUsername();

        String publicKey = payload.get("publicKey");
        userService.savePublicKey(username, publicKey);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String username,
            @RequestBody UpdateUserRequest updateUserRequest,
            Authentication authentication) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        boolean isSelf = userEntityPrincipal.getUsername().equals(username);
        if (!isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        UserDTO updatedUser = userService.updateUser(username, updateUserRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username, Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        boolean isSelf = userEntityPrincipal.getUsername().equals(username);

        // Chỉ cho phép tự xóa. Admin muốn xóa phải dùng API /admin/delete/{username}
        if (!isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        String username = userEntityPrincipal.getUsername();

        userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
