package com.web.backend.controller;

import com.web.backend.controller.request.SaveKeyRequest;
import com.web.backend.controller.request.SavePublicKeyRequest;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.controller.response.RsaKeyResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.KeyService;
import com.web.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@Slf4j(topic = "KEY-CONTROLLER")
public class KeyController {

    private final KeyService keyService;

    @GetMapping("/rsa")
    public ResponseEntity<ApiResponse<RsaKeyResponse>> getRsaKey(Authentication auth) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        log.info("Fetching RSA key for user: {}", user.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Get RSA key successfully",
                        RsaKeyResponse.builder().privateKey(keyService.getRsaKey(user.getUsername())).build()
                        )
        );
    }

    @PostMapping("/rsa")
    public ResponseEntity<ApiResponse<Void>> saveRsaKey(
            Authentication auth,
            @RequestBody @Valid SaveKeyRequest request) {

        UserEntity user = (UserEntity) auth.getPrincipal();

        log.info("Saving RSA key for user: {}", user.getUsername());

        keyService.saveRsaKey(user.getUsername(), request.getKey());

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "RSA key saved successfully", null)
        );
    }

    @GetMapping("/public-key/{username}")
    public ResponseEntity<ApiResponse<String>> getPublicKey(Authentication authentication, @PathVariable String username) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Get public key: {}", userEntityPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Lấy khóa công khai thành công",
                keyService.getPublicKey(username)));
    }

    @PostMapping("/public-key")
    public ResponseEntity<ApiResponse<Void>> savePublicKey(Authentication authentication, @RequestBody @Valid SavePublicKeyRequest request) {
        UserEntity userEntityPrincipal = (UserEntity) authentication.getPrincipal();
        log.info("Saved public key for user: {}", userEntityPrincipal.getUsername());
        keyService.savePublicKey(userEntityPrincipal.getUsername(), request.getPublicKey());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Lưu khóa công khai thành công", null));
    }

}