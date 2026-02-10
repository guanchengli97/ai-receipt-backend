package com.example.aireceiptbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String activationLinkBase;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${spring.mail.properties.mail.smtp.from}") String fromAddress,
        @Value("${app.auth.activation-link-base:https://aireceipt.guanchengli.com/activate}") String activationLinkBase
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.activationLinkBase = activationLinkBase;
    }

    public void sendActivationEmail(String toEmail, String username, String token) {
        String activationUrl = activationLinkBase + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Activate your AI Receipt account");
        message.setText(
            "Hi " + username + ",\n\n" +
            "Please activate your account by clicking the link below:\n" +
            activationUrl + "\n\n" +
            "This link expires in 24 hours.\n\n" +
            "If you did not register for AI Receipt, please ignore this email."
        );

        mailSender.send(message);
    }
}
