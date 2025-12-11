package com.web.backend.config;

import com.web.backend.common.UserStatus;
import com.web.backend.model.PermissionEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.PermissionRepository;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        PermissionEntity pUserView = createPermissionIfNotFound("USER_VIEW", "Xem danh sách user");
        PermissionEntity pUserCreate = createPermissionIfNotFound("USER_CREATE", "Tạo user mới");
        PermissionEntity pUserUpdate = createPermissionIfNotFound("USER_UPDATE", "Sửa user");
        PermissionEntity pUserDelete = createPermissionIfNotFound("USER_DELETE", "Xóa user");

        RoleEntity roleAdmin = createRoleIfNotFound("ADMIN", "Quản trị viên hệ thống");
        RoleEntity roleUser = createRoleIfNotFound("USER", "Người dùng cơ bản");

        assignPermissionToRole(roleAdmin, pUserView, pUserCreate, pUserUpdate, pUserDelete);

        assignPermissionToRole(roleUser, pUserView);

        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@example.com");
            admin.setUserStatus(UserStatus.ACTIVE);
            admin.setFirstName("Super");
            admin.setLastName("Admin");

            admin.setRole(roleAdmin);

            userRepository.save(admin);
        }
    }

    private PermissionEntity createPermissionIfNotFound(String name, String desc) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    PermissionEntity p = new PermissionEntity();
                    p.setName(name);
                    p.setDescription(desc);
                    return permissionRepository.save(p);
                });
    }

    private RoleEntity createRoleIfNotFound(String name, String desc) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity();
                    r.setName(name);
                    r.setDescription(desc);
                    return roleRepository.save(r);
                });
    }

    private void assignPermissionToRole(RoleEntity role, PermissionEntity... permissions) {
        for (PermissionEntity p : permissions) {
            role.getPermissions().add(p);
        }
        roleRepository.save(role);
    }
}