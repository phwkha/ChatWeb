package com.web.backend.config;

import com.mongodb.RequestContext;
import com.web.backend.common.UserStatus;
import com.web.backend.model.PermissionEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.PermissionRepository;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.CuckooFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j(topic = "DATABASE-SEEDER")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CuckooFilterService cuckooFilterService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("import data start");
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
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setUserStatus(UserStatus.ACTIVE);
            admin.setFirstName("Super");
            admin.setLastName("Admin");

            admin.setRole(roleAdmin);

            userRepository.save(admin);
        }
        log.info("import data end");

        if (!redisTemplate.hasKey("filter:emails")) {
            log.info("Initializing Cuckoo Filter...");
            List<UserEntity> allUsers = userRepository.findAll();
            for (UserEntity u : allUsers) {
                cuckooFilterService.add("filter:emails", u.getEmail());
                cuckooFilterService.add("filter:usernames", u.getUsername());
            }
            log.info("Cuckoo Filter initialized with {} users", allUsers.size());
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

    @Bean
    public CommandLineRunner cleanupOnlineStatus() {
        return args -> {
            String ONLINE_USERS_KEY = "online_users";
            String ONLINE_USERS_COUNT_KEY = "online_users_count";

            if (Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_KEY))) {
                redisTemplate.delete(ONLINE_USERS_KEY);
            }
            if (Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_COUNT_KEY))) {
                redisTemplate.delete(ONLINE_USERS_COUNT_KEY);
            }
            log.info(">>> CLEANUP: Đã reset trạng thái Online Users trong Redis để tránh dữ liệu ảo.");
        };
    }
}