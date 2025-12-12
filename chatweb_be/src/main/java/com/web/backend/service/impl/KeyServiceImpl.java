package com.web.backend.service.impl;

import com.web.backend.common.UserStatus;
import com.web.backend.exception.AccessForbiddenException;
import com.web.backend.exception.ResourceNotFoundException;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.KeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j(topic = "KEY-SERVICE")
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void saveRsaKey(String username, String encryptedKey) {

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        userEntity.setEncryptedRsaPrivateKey(encryptedKey);

        userRepository.save(userEntity);
        log.info("RSA key saved successfully for user: {}", username);
    }

    @Override
    public String getRsaKey(String username) {

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));

        if (userEntity.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessForbiddenException("Tài khoản đã bị khóa hoặc không kích hoạt.");
        }

        String key = userEntity.getEncryptedRsaPrivateKey();
        if (key == null) {
            log.warn("RSA key not found for user: {}", username);
            return null;
        }
        log.debug("Fetching RSA key for user: {}", username);
        return key;
    }
}