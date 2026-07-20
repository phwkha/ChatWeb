package com.web.backend.kafka.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailPayload {

    private static final String OTP_STRING = "OTP";

    private static final String TEXT_STRING = "TEXT";

    private String type;

    private String to;

    private String name;
    private String otp;

    private String subject;
    private String content;

    public static EmailPayload createOtpEvent(String to, String name, String otp) {
        return new EmailPayload(OTP_STRING, to, name, otp, null, null);
    }

    public static EmailPayload createTextEvent(String to, String subject, String content) {
        return new EmailPayload(TEXT_STRING, to, null, null, subject, content);
    }
}