package com.web.backend.config;

import com.web.backend.common.UserStatus;
import com.web.backend.model.PermissionEntity;
import com.web.backend.model.RoleEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.PermissionRepository;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.util.CuckooFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j(topic = "DATABASE-SEEDER")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final PasswordEncoder passwordEncoder;

    private final CuckooFilterService cuckooFilterService;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ADMIN123_STRING = "admin123";
    private static final String ADMIN_2_STRING = "admin";
    private static final String ADMIN_3_STRING = "Admin";
    private static final String ADMIN_EXAMPLE_COM_STRING = "admin@example.com";
    private static final String NG_I_D_NG_C_B_N_STRING = "Người dùng cơ bản";
    private static final String ONLINE_USERS_COUNT_STRING = "online_users_count";
    private static final String ONLINE_USERS_STRING = "online_users";
    private static final String QU_N_TR_VI_N_H_TH_NG_STRING = "Quản trị viên hệ thống";
    private static final String SUPER_STRING = "Super";
    private static final String S_A_USER_STRING = "Sửa user";
    private static final String T_O_USER_M_I_STRING = "Tạo user mới";
    private static final String USER_CREATE_STRING = "USER_CREATE";
    private static final String USER_DELETE_STRING = "USER_DELETE";
    private static final String USER_STRING = "USER";
    private static final String USER_UPDATE_STRING = "USER_UPDATE";
    private static final String USER_VIEW_STRING = "USER_VIEW";
    private static final String XEM_DANH_S_CH_USER_STRING = "Xem danh sách user";
    private static final String X_A_USER_STRING = "Xóa user";
    private static final String ADMIN_STRING = "ADMIN";

    private static final String FILTER_EMAILS_STRING = "filter:emails";
    private static final String FILTER_USERNAMES_STRING = "filter:usernames";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("import data start");
        PermissionEntity pUserView = createPermissionIfNotFound(USER_VIEW_STRING, XEM_DANH_S_CH_USER_STRING);
        PermissionEntity pUserCreate = createPermissionIfNotFound(USER_CREATE_STRING, T_O_USER_M_I_STRING);
        PermissionEntity pUserUpdate = createPermissionIfNotFound(USER_UPDATE_STRING, S_A_USER_STRING);
        PermissionEntity pUserDelete = createPermissionIfNotFound(USER_DELETE_STRING, X_A_USER_STRING);

        RoleEntity roleAdmin = createRoleIfNotFound(ADMIN_STRING, QU_N_TR_VI_N_H_TH_NG_STRING);
        RoleEntity roleUser = createRoleIfNotFound(USER_STRING, NG_I_D_NG_C_B_N_STRING);

        assignPermissionToRole(roleAdmin, pUserView, pUserCreate, pUserUpdate, pUserDelete);

        assignPermissionToRole(roleUser, pUserView);

        if (!userRepository.existsByUsername(ADMIN_2_STRING)) {
            UserEntity admin = new UserEntity();
            admin.setUsername(ADMIN_2_STRING);
            admin.setPassword(passwordEncoder.encode(ADMIN123_STRING));
            admin.setEmail(ADMIN_EXAMPLE_COM_STRING);
            admin.setUserStatus(UserStatus.ACTIVE);
            admin.setFirstName(SUPER_STRING);
            admin.setLastName(ADMIN_3_STRING);

            admin.setRole(roleAdmin);

            userRepository.save(admin);
        }
        log.info("import data end");

        if (!redisTemplate.hasKey(FILTER_EMAILS_STRING)) {
            log.info("Initializing Cuckoo Filter...");
            List<UserEntity> allUsers = userRepository.findAll();
            for (UserEntity u : allUsers) {
                cuckooFilterService.add(FILTER_EMAILS_STRING, u.getEmail());
                cuckooFilterService.add(FILTER_USERNAMES_STRING, u.getUsername());
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
        if (role == null)
            return;
        for (PermissionEntity p : permissions) {
            role.getPermissions().add(p);
        }
        roleRepository.save(role);
    }

    @Bean
    public CommandLineRunner cleanupOnlineStatus() {
        return args -> {
            String ONLINE_USERS_KEY = ONLINE_USERS_STRING;
            String ONLINE_USERS_COUNT_KEY = ONLINE_USERS_COUNT_STRING;

            if (Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_KEY))) {
                redisTemplate.delete(ONLINE_USERS_KEY);
            }
            if (Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_COUNT_KEY))) {
                redisTemplate.delete(ONLINE_USERS_COUNT_KEY);
            }
            log.info(">>> CLEANUP: Reset Online Users state in Redis to avoid phantom data.");
        };
    }
}