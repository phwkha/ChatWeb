package com.web.backend.repository;

import com.web.backend.common.UserStatus;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity,Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByUsernameIn(Collection<String> usernames);

    Page<UserEntity> findAllByUserStatusNot(UserStatus status, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(RoleEntity role);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.isOnline = :isOnline WHERE u.username = :username")
    void updateOnlineStatus(String username, boolean isOnline);
}
