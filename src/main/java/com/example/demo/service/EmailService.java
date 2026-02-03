package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Async
    public void sendVerificationEmail(String to, String code) {
        log.info("Sending verification email to: {}", to);
        SimpleMailMessage message =new SimpleMailMessage();
        message.setFrom(fromMail);
        message.setTo(to);
        message.setSubject("Lost & Found - Email Verification");
        message.setText(
                "Your verification code is: " + code + "\n\n" +
                        "This code expires in 15 minutes.\n\n" +
                        "If you didn't request this, please ignore this email."
        );

        mailSender.send(message);
    }
}
