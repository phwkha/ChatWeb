package com.web.backend.repository;

import com.web.backend.common.UserStatus;
import com.web.backend.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity,Long> {

    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findByIsOnlineTrue();

    Page<UserEntity> findAllByUserStatusNot(UserStatus status, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserEntity getUserEntityByUsername(String username);
}
