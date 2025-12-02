package com.web.backend.repository;

import com.web.backend.common.UserStatus;
import com.web.backend.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity,Long> {

    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findByIsOnlineTrue();

    List<UserEntity> findAllByUserStatusNot(UserStatus status);

    boolean existsByUsername(String username);
}
