package com.web.backend.service;

public interface KeyService {
    void saveRsaKey(String username, String encryptedKey);
    String getRsaKey(String username);
}