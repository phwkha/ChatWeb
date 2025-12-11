package com.web.backend.repository;

import com.web.backend.common.OtpType;
import com.web.backend.model.UserEntity;
import com.web.backend.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByUserAndType(UserEntity user, OtpType type);
}