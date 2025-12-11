package com.web.backend.controller;

import com.web.backend.controller.request.VerifyOtpRequest;
import com.web.backend.controller.response.ApiResponse;
import com.web.backend.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        log.info("Verify Otp Request: {}", request);
        otpService.verifyUser(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.", null));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email) {
        log.info("Resend Otp Request: {}", email);
        otpService.resendOtp(email);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Đã gửi lại mã OTP.", null));
    }

}
