package com.web.backend.service.util;

import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE-DETAIL")
public class UserServiceDetail implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "user_details", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        if (!user.isAccountNonLocked()) {
            log.info("user locked: {}", username);
            throw new LockedException("Tài khoản đã bị khóa. Vui lòng liên hệ Admin.");
        }

        if (!user.isEnabled()) {
            log.info("user not found: {}", username);
            throw new DisabledException("Người dùng không tồn tại");
        }

        return user;
    }
}