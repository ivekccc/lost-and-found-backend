package com.example.demo.notification.service;

import org.springframework.context.i18n.LocaleContextHolder;

import org.springframework.context.MessageSource;

import com.example.demo.shared.exception.EmailSendException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;

    @Value("${spring.mail.username}")
    private String fromMail;


    public void sendVerificationEmail(String to, String code) {
        try {
          MimeMessage message = mailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

          helper.setFrom(fromMail);
          helper.setTo(to);
          helper.setSubject(messageSource.getMessage("email.verification.subject", null, LocaleContextHolder.getLocale()));
          helper.setText(buildVerificationEmailHtml(code), true);

          mailSender.send(message);
          log.info("Verification email sent to: {}", to);
      } catch (Exception e) {
          log.error("Failed to send verification email to: {}", to, e);
          throw new EmailSendException("email.verificationSendFailed");
      }
    }
    private String buildVerificationEmailHtml(String code) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("codeChars", code.chars().mapToObj(c -> String.valueOf((char) c)).toList());
        return templateEngine.process("email/verification", context);
    }

    public void sendPasswordResetEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromMail);
            helper.setTo(to);
            helper.setSubject(messageSource.getMessage("email.passwordReset.subject", null, LocaleContextHolder.getLocale()));
            helper.setText(buildPasswordResetEmailHtml(code), true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new EmailSendException("email.passwordResetSendFailed");
        }
    }

    private String buildPasswordResetEmailHtml(String code) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("codeChars", code.chars().mapToObj(c -> String.valueOf((char) c)).toList());
        return templateEngine.process("email/password-reset", context);
    }
}
