package com.web.backend.repository;

import com.web.backend.model.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByName(String name);
}
