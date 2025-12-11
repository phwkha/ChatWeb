package com.web.backend.controller;

import com.web.backend.controller.request.SaveKeyRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.RsaKeyResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.service.KeyService;
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
@Slf4j
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
}