package com.example.demo.notification.service;

import com.example.demo.shared.exception.EmailSendException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
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
          log.info("Verification email sent");
      } catch (Exception e) {
          log.error("Failed to send verification email", e);
          throw new EmailSendException("email.verificationSendFailed");
      }
    }
    private String buildVerificationEmailHtml(String code) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("codeChars", code.chars().mapToObj(c -> String.valueOf((char) c)).toList());
        return templateEngine.process("email/verification", context);
    }

    public void sendPasswordResetEmail(String to, String code, Locale locale) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromMail);
            helper.setTo(to);
            helper.setSubject(messageSource.getMessage("email.passwordReset.subject", null, locale));
            helper.setText(buildPasswordResetEmailHtml(code, locale), true);

            mailSender.send(message);
            log.info("Password reset email sent");
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new EmailSendException("email.passwordResetSendFailed");
        }
    }

    private String buildPasswordResetEmailHtml(String code, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("codeChars", code.chars().mapToObj(c -> String.valueOf((char) c)).toList());
        return templateEngine.process("email/password-reset", context);
    }
}
