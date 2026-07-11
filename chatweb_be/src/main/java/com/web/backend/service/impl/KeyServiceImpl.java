package com.web.backend.service.impl;

import com.web.backend.common.UserStatus;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.KeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.web.backend.config.LocalResolverConfig.Translator;

@Service
@Slf4j(topic = "KEY-SERVICE")
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void saveRsaKey(String username, String encryptedKey) {

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username)));

        userEntity.setEncryptedRsaPrivateKey(encryptedKey);

        userRepository.save(userEntity);
        log.info("RSA key saved successfully for user: {}", username);
    }

    @Override
    public String getRsaKey(String username) {

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username)));

        if (userEntity.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessForbiddenException(Translator.tolocale("error.key.locked_inactive"));
        }

        String key = userEntity.getEncryptedRsaPrivateKey();
        if (key == null) {
            log.warn("RSA key not found for user: {}", username);
            return null;
        }
        log.debug("Fetching RSA key for user: {}", username);
        return key;
    }

    @Override
    public String getPublicKey(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username)));
        log.info("Get public key");
        return userEntity.getPublicKey();
    }

    @Override
    @CacheEvict(value = "user_details", key = "#username")
    public void savePublicKey(String username, String publicKey) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.tolocale("error.user.not_found_with", username)));
        userEntity.setPublicKey(publicKey);
        userRepository.save(userEntity);
        log.info("Saved public key for user");
    }
}