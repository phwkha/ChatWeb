package com.web.backend.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEmailEvent {
    private String type;

    private String to;

    private String name;
    private String otp;

    private String subject;
    private String content;

    public static KafkaEmailEvent createOtpEvent(String to, String name, String otp) {
        return new KafkaEmailEvent("OTP", to, name, otp, null, null);
    }

    public static KafkaEmailEvent createTextEvent(String to, String subject, String content) {
        return new KafkaEmailEvent("TEXT", to, null, null, subject, content);
    }
}