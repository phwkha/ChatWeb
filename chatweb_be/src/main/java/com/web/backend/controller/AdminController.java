package com.web.backend.controller;

import com.web.backend.common.Role;
import com.web.backend.controller.request.AdminCreateUserRequest;
import com.web.backend.controller.request.AdminUpdateUserRequest;
import com.web.backend.model.DTO.UserDTO;
import com.web.backend.model.UserEntity;
import com.web.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/add")
    public ResponseEntity<UserDTO> addUser(@RequestBody AdminCreateUserRequest adminCreateUserRequest, Authentication authentication) {
        UserDTO newUser = userService.adminCreateUser(adminCreateUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PostMapping("/{username}/unlock")
    public ResponseEntity<UserDTO> unlockUser(@PathVariable String username, Authentication authentication) {
        UserDTO unlockedUser = userService.unlockUser(username);
        return ResponseEntity.ok(unlockedUser);
    }

    @PostMapping("/{username}/lock")
    public ResponseEntity<UserDTO> lockUser(@PathVariable String username, Authentication authentication) {
        UserDTO lockedUser = userService.lockUser(username);
        return ResponseEntity.ok(lockedUser);
    }

    @PutMapping("/{username}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String username,
            @RequestBody AdminUpdateUserRequest adminUpdateUserRequest) {
        UserDTO updatedUser = userService.adminUpdateUser(username, adminUpdateUserRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, Authentication authentication) {
        UserDTO user = userService.getUserByUsername(username);
        UserEntity adminPrincipal = (UserEntity) authentication.getPrincipal();
        if (user.getRole() == Role.ADMIN_PRO) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("khong the xoa adminpro");
        }
        if (user.getRole() == Role.ADMIN) {
            if (adminPrincipal.getRole() == Role.ADMIN_PRO) {
                userService.deleteUser(username);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Chỉ ADMIN_PRO mới có quyền xóa tài khoản ADMIN!");
            }
        }

        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
