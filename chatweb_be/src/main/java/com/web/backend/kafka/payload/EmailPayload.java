package com.web.backend.kafka.payload;

public record EmailPayload(
    String type,
    String to,
    String name,
    String otp,
    String subject,
    String content
) {

    private static final String OTP_STRING = "OTP";
    private static final String TEXT_STRING = "TEXT";

    public static EmailPayload createOtpEvent(String to, String name, String otp) {
        return new EmailPayload(OTP_STRING, to, name, otp, null, null);
    }

    public static EmailPayload createTextEvent(String to, String subject, String content) {
        return new EmailPayload(TEXT_STRING, to, null, null, subject, content);
    }
}