package com.web.backend.service;

public interface KeyService {

    void saveRsaKey(String username, String encryptedKey);

    String getRsaKey(String username);

    void savePublicKey(String username, String publicKey);

    String getPublicKey(String username);
}