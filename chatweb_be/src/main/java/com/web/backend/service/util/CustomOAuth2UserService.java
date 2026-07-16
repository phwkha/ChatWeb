package com.web.backend.service.util;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.web.backend.common.AuthProvider;
import com.web.backend.common.UserStatus;
import com.web.backend.model.UserEntity;
import com.web.backend.oauth2.CustomOAuth2User;
import com.web.backend.repository.RoleRepository;
import com.web.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = extractProviderId(oAuth2User, registrationId);
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isEmpty()) {
            log.error("OAuth2 login failed: Provider returned empty email for providerId {}", providerId);
            throw new OAuth2AuthenticationException("error.oauth2.email_missing");
        }

        log.info("Processing OAuth2 login for user email: {}", email);
        UserEntity user = processOAuth2User(userRequest, oAuth2User, providerId, email);
        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return oAuth2User.getAttribute("sub");
        } else if ("github".equalsIgnoreCase(registrationId) || "facebook".equalsIgnoreCase(registrationId)) {
            Object idAttr = oAuth2User.getAttribute("id");
            return idAttr != null ? String.valueOf(idAttr) : null;
        }

        Object sub = oAuth2User.getAttribute("sub");
        if (sub != null)
            return String.valueOf(sub);
        Object id = oAuth2User.getAttribute("id");
        return id != null ? String.valueOf(id) : null;
    }

    private UserEntity processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User, String providerId,
            String email) {
        Optional<UserEntity> userByProviderId = userRepository.findByProviderId(providerId);

        if (userByProviderId.isPresent()) {
            log.info("User found by providerId: {}", providerId);
            return userByProviderId.get();
        }

        Optional<UserEntity> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            log.info("User found by email {}. Linking account with new providerId: {}", email, providerId);
            UserEntity existingUser = userByEmail.get();
            existingUser.setProviderId(providerId);
            existingUser.setAuthProvider(determineAuthProvider(userRequest));
            return userRepository.save(existingUser);
        }

        log.info("User not found. Registering new user with email: {}", email);
        return registerNewUser(userRequest, oAuth2User, providerId, email);
    }

    private UserEntity registerNewUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User, String providerId,
            String email) {
        UserEntity newUser = new UserEntity();

        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setProviderId(providerId);
        newUser.setAuthProvider(determineAuthProvider(userRequest));
        newUser.setUserStatus(UserStatus.ACTIVE);

        String username = oAuth2User.getAttribute("name");
        if (username != null) {
            username = username.toString().replaceAll("\\s+", "");
        } else {
            username = "user";
        }
        newUser.setUsername(username + "_" + System.currentTimeMillis());

        newUser.setRole(roleRepository.findByName("USER")
                .orElseThrow(() -> {
                    log.error("Failed to assign role: Default role 'USER' not found in database");
                    return new OAuth2AuthenticationException("error.role.not_found");
                }));

        newUser.setFirstName(oAuth2User.getAttribute("given_name"));
        newUser.setLastName(oAuth2User.getAttribute("family_name"));
        newUser.setAvatar(oAuth2User.getAttribute("picture"));

        return userRepository.save(newUser);
    }

    private AuthProvider determineAuthProvider(OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        try {
            return AuthProvider.valueOf(registrationId);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown AuthProvider '{}'. Falling back to GOOGLE", registrationId);
            return AuthProvider.GOOGLE;
        }
    }
}
