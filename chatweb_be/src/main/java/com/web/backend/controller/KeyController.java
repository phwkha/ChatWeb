package com.web.backend.controller;

import com.web.backend.model.UserEntity;
import com.web.backend.service.KeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class KeyController {

    private final KeyService keyService;

    @GetMapping("/rsa")
    public ResponseEntity<Map<String, String>> getRsaKey(Authentication auth) {
        String username = ((UserEntity) auth.getPrincipal()).getUsername();
        String key = keyService.getRsaKey(username);
        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rsa")
    public ResponseEntity<Void> saveRsaKey(Authentication auth, @RequestBody Map<String, String> payload) {
        String username = ((UserEntity) auth.getPrincipal()).getUsername();
        keyService.saveRsaKey(username, payload.get("key"));
        return ResponseEntity.ok().build();
    }
}