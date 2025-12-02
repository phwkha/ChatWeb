package com.web.backend.service.impl;

import com.web.backend.JWT.JwtService;
import com.web.backend.common.Convert;
import com.web.backend.common.UserStatus;
import com.web.backend.controller.request.LoginRequest;
import com.web.backend.controller.response.LoginResponse;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;


@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        UserEntity userEntity = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")); // Sai username

        // 2. Kiểm tra logic xóa mềm (INACTIVE)
        if (userEntity.getUserStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("User not found"); // Giả vờ không tìm thấy
        }

        // 3. Xác thực bằng AuthenticationManager
        // Nếu user bị LOCKED (đã cấu hình ở bước 2), hàm này sẽ ném LockedException
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new RuntimeException("Sai mật khẩu hoặc tài khoản không tồn tại");
        }

        String jwtToken = jwtService.generateToken(userEntity);

        return LoginResponse.builder()
                .token(jwtToken)
                .userDTO(Convert.UserConvertToUserDTO(userEntity))
                .build();
    }

    @Override
    public ResponseEntity<String> logout() {
        ResponseCookie responseCookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body("Logged out successfully");
    }

}
