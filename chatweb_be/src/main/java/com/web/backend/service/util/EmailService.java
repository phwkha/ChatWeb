package com.web.backend.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j(topic = "EMAIL-SERVICE")
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTextEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email sent successfully via Gmail to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}. Error: {}", to, e.getMessage());
            throw e;
        }
    }

    public void sendOtpEmail(String to, String name, String otp) {
        log.info("Sending OTP via Gmail to: {}", to);

        String subject = "ChatWeb - Mã Xác Nhận OTP";
        String content = "Xin chào " + name + ",\n\n" +
                "Mã xác nhận (OTP) của bạn là: " + otp + "\n\n" +
                "Vui lòng không chia sẻ mã này cho bất kỳ ai. Mã có hiệu lực trong 5 phút.\n\n" +
                "Trân trọng,\nĐội ngũ ChatWeb";

        sendTextEmail(to, subject, content);
    }
}