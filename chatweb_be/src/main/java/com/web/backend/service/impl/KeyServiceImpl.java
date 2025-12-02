package com.web.backend.service.impl;

import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.KeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {

    private final UserRepository userRepository;


    @Override
    public void saveRsaKey(String username, String encryptedKey) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userEntity.setEncryptedRsaPrivateKey(encryptedKey);
        userRepository.save(userEntity);
    }

    @Override
    public String getRsaKey(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userEntity.getEncryptedRsaPrivateKey();
    }
}