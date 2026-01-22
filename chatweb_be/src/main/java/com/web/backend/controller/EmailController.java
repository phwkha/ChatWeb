package com.web.backend.controller;

import com.web.backend.controller.request.EmailRequest;
import com.web.backend.controller.response.form.ApiResponse;
import com.web.backend.service.util.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-CONTROLLER")
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<Void>> sendEmail(@RequestBody @Valid EmailRequest request) {
        log.info("Request to send email to: {}", request.getTo());

        emailService.sendTextEmail(request.getTo(), request.getSubject(), request.getText());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Email đang được gửi đi",
                null
        ));
    }
}