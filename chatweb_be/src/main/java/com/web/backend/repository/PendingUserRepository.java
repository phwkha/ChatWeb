package com.web.backend.repository;

import com.web.backend.model.PendingUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUserEntity, Long> {
    Optional<PendingUserEntity> findByEmail(String email);
    Optional<PendingUserEntity> findByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM PendingUserEntity p WHERE p.otpExpiry < :time")
    void deleteExpiredRequests(LocalDateTime time);
}