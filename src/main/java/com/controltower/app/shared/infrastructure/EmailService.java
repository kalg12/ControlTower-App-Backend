package com.controltower.app.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender.
 * If mail is not configured, logs a warning and silently skips sending.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@controltower.io}")
    private String from;

    public void sendPasswordReset(String toEmail, String resetLink) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Reset your Control Tower password");
            message.setText("Click the link below to reset your password (expires in 1 hour):\n\n"
                    + resetLink + "\n\nIf you did not request a password reset, ignore this email.");
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendWelcome(String toEmail, String fullName) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Welcome email for {} ({})", toEmail, fullName);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Welcome to Control Tower!");
            message.setText("Hi " + fullName + ",\n\nYour Control Tower account has been created. "
                    + "You can now log in and start managing your operations.\n\nWelcome aboard!");
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send welcome email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
