package com.web.backend.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j(topic = "EMAIL-SERVICE")
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SendGrid sendGrid;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmail;

    @Value("${spring.sendgrid.otp}")
    private String otpTemplateId;

    @Async
    public void sendTextEmail(String to, String subject, String content) {

        Email from = new Email(fromEmail);
        Email toAddress = new Email(to);
        Content emailContent = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, toAddress, emailContent);

        sendInternal(mail);
        log.info("Sending plain text email to: {}", to);
    }


    @Async
    public void sendOtpEmail(String to, String name, String otp) {
        log.info("Sending OTP email to: {}", to);

        Email from = new Email(fromEmail);
        Email toAddress = new Email(to);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setTemplateId(otpTemplateId);

        Personalization personalization = new Personalization();
        personalization.addTo(toAddress);

        personalization.addDynamicTemplateData("name", name);
        personalization.addDynamicTemplateData("otp", otp);

        mail.addPersonalization(personalization);

        sendInternal(mail);
    }

    private void sendInternal(Mail mail) {
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully. Status: {}", response.getStatusCode());
            } else {
                log.error("Failed to send email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Error connecting to SendGrid: {}", e.getMessage());
        }
    }
}