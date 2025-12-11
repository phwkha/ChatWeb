package com.web.backend.service;

import com.web.backend.repository.PendingUserRepository;
import com.web.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCleanupService {

    private final PendingUserRepository pendingUserRepository;

    @Scheduled(fixedRate = 3600000)
    public void cleanupInactiveUsers() {
        log.info("Cleaning up expired registration requests...");
        pendingUserRepository.deleteExpiredRequests(LocalDateTime.now());
    }
}