package com.controltower.app.email.application;

import com.controltower.app.email.domain.EmailDelivery;
import com.controltower.app.email.domain.EmailDeliveryRepository;
import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.email.domain.EmailMailboxConfigRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboundService {

    private final EmailDeliveryRepository deliveryRepo;
    private final EmailMailboxConfigRepository mailboxRepo;
    private final AesEncryptor aesEncryptor;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MINUTES = 10;

    // ── Public API ────────────────────────────────────────────────────────────

    public EmailDelivery sendTicketReply(UUID tenantId, UUID mailboxId, UUID ticketId,
                                          String toEmail, String subject, String bodyHtml,
                                          String inReplyToMessageId) {
        EmailDelivery delivery = new EmailDelivery();
        delivery.setTenantId(tenantId);
        delivery.setMailboxId(mailboxId);
        delivery.setTicketId(ticketId);
        delivery.setToEmail(new String[]{toEmail});
        delivery.setSubject(subject);
        delivery.setBodyHtml(bodyHtml);
        delivery.setInReplyTo(inReplyToMessageId);
        delivery.setDeliveryType(EmailDelivery.DeliveryType.TICKET_REPLY);
        delivery.setStatus(EmailDelivery.DeliveryStatus.QUEUED);
        deliveryRepo.save(delivery);
        attempt(delivery);
        return delivery;
    }

    public EmailDelivery sendTest(UUID tenantId, UUID mailboxId, String toEmail,
                                   String subject, String bodyHtml) {
        EmailMailboxConfig config = mailboxRepo.findById(mailboxId)
                .orElseThrow(() -> new ControlTowerException(
                        "Mailbox not found: " + mailboxId, HttpStatus.NOT_FOUND));

        EmailDelivery delivery = new EmailDelivery();
        delivery.setTenantId(tenantId);
        delivery.setMailboxId(mailboxId);
        delivery.setFromEmail(config.getFromEmail());
        delivery.setToEmail(new String[]{toEmail});
        delivery.setSubject(subject);
        delivery.setBodyHtml(bodyHtml != null ? bodyHtml : buildTestBody(config));
        delivery.setDeliveryType(EmailDelivery.DeliveryType.TEST);
        delivery.setStatus(EmailDelivery.DeliveryStatus.QUEUED);
        deliveryRepo.save(delivery);
        attempt(delivery, config);
        return delivery;
    }

    public void attempt(EmailDelivery delivery) {
        EmailMailboxConfig config = mailboxRepo.findById(delivery.getMailboxId()).orElse(null);
        if (config == null) {
            markFailed(delivery, "SMTP_CONFIG_ERROR: Mailbox config not found: " + delivery.getMailboxId());
            deliveryRepo.save(delivery);
            return;
        }
        attempt(delivery, config);
    }

    void attempt(EmailDelivery delivery, EmailMailboxConfig config) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastAttemptAt(Instant.now());

        warnIfFromAuthMismatch(config);

        try {
            doSend(delivery, config);
            delivery.setStatus(EmailDelivery.DeliveryStatus.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setErrorMessage(null);
            log.info("[SMTP] Delivered — to={} host={}:{} delivery={}",
                    delivery.getToEmail()[0], config.getSmtpHost(), config.getSmtpPort(), delivery.getId());
        } catch (Exception e) {
            String code = classifySmtpError(e);
            String classified = code + ": " + e.getMessage();
            log.warn("[SMTP] Failed — mailboxId={} host={}:{} ssl={} from={} to={} attempt={}/{} code={} msg={}",
                    delivery.getMailboxId(), config.getSmtpHost(), config.getSmtpPort(),
                    config.isSmtpSsl(), config.getFromEmail(),
                    String.join(",", delivery.getToEmail()),
                    delivery.getAttempts(), MAX_RETRY_ATTEMPTS, code, e.getMessage());
            if (delivery.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                markFailed(delivery, classified);
            } else {
                delivery.setNextRetryAt(Instant.now().plusSeconds(RETRY_BACKOFF_MINUTES * 60L * delivery.getAttempts()));
                delivery.setErrorMessage(classified);
            }
        }

        deliveryRepo.save(delivery);
    }

    // ── Core send ─────────────────────────────────────────────────────────────

    private void doSend(EmailDelivery delivery, EmailMailboxConfig config) throws Exception {
        JavaMailSenderImpl sender = buildSender(config);
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(config.getFromEmail(),
                config.getFromName() != null ? config.getFromName() : config.getFromEmail());
        helper.setTo(delivery.getToEmail());

        if (delivery.getCcEmail() != null && delivery.getCcEmail().length > 0) {
            helper.setCc(delivery.getCcEmail());
        }

        if (delivery.getTicketId() != null) {
            String domain = domainOf(config.getFromEmail());
            helper.setReplyTo("ticket+" + delivery.getTicketId() + "@" + domain);
        }

        helper.setSubject(delivery.getSubject() != null ? delivery.getSubject() : "(Sin asunto)");
        helper.setText(delivery.getBodyHtml() != null ? delivery.getBodyHtml() : "", true);

        if (delivery.getInReplyTo() != null) {
            message.setHeader("In-Reply-To", delivery.getInReplyTo());
            message.setHeader("References", delivery.getInReplyTo());
        }

        if (delivery.getDeliveryType() == EmailDelivery.DeliveryType.TICKET_REPLY) {
            message.setHeader("X-Auto-Submitted", "auto-replied");
        }

        sender.send(message);
    }

    // ── SMTP builder ──────────────────────────────────────────────────────────

    private JavaMailSenderImpl buildSender(EmailMailboxConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setUsername(config.getSmtpUsername());
        sender.setPassword(aesEncryptor.decrypt(config.getSmtpPassword()));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        if (config.getSmtpPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else if (config.isSmtpSsl()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
        }

        return sender;
    }

    // ── Error classification ──────────────────────────────────────────────────

    public String classifySmtpError(Exception e) {
        String msg = (e.getMessage() != null ? e.getMessage() : "").toLowerCase();
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            msg += " " + e.getCause().getMessage().toLowerCase();
        }
        if (msg.contains("535") || msg.contains("534") || msg.contains("authentication")
                || msg.contains("username and password") || msg.contains("invalid credentials")) {
            return "SMTP_AUTH_FAILED";
        }
        if (msg.contains("tls") || msg.contains("ssl") || msg.contains("handshake")
                || msg.contains("certificate") || msg.contains("plaintext")) {
            return "SMTP_TLS_FAILED";
        }
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("read timeout")) {
            return "SMTP_TIMEOUT";
        }
        if (msg.contains("connect") || msg.contains("refused") || msg.contains("unreachable")
                || msg.contains("no route") || msg.contains("unknown host") || msg.contains("network")) {
            return "SMTP_CONNECTION_FAILED";
        }
        if (msg.contains("550") || msg.contains("551") || msg.contains("recipient")
                || msg.contains("user unknown") || msg.contains("no such user")) {
            return "SMTP_RECIPIENT_REJECTED";
        }
        if (msg.contains("553") || msg.contains("sender rejected") || msg.contains("from address")) {
            return "SMTP_SENDER_REJECTED";
        }
        if (msg.contains("452") || msg.contains("rate limit") || msg.contains("too many") || msg.contains("quota")) {
            return "SMTP_RATE_LIMITED";
        }
        if (msg.contains("locked") || msg.contains("blocked") || msg.contains("disabled") || msg.contains("suspended")) {
            return "SMTP_ACCOUNT_LOCKED";
        }
        return "SMTP_UNKNOWN_ERROR";
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void warnIfFromAuthMismatch(EmailMailboxConfig config) {
        if (config.getFromEmail() == null || config.getSmtpUsername() == null) return;
        String fromDomain = domainOf(config.getFromEmail());
        String authDomain = domainOf(config.getSmtpUsername());
        if (!fromDomain.equalsIgnoreCase(authDomain)) {
            log.warn("[SMTP] From/auth domain mismatch — From={} AuthUser={} — " +
                    "Hotmail/Outlook often rejects cross-domain sends silently",
                    config.getFromEmail(), config.getSmtpUsername());
        }
    }

    private static String domainOf(String email) {
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1).toLowerCase() : email.toLowerCase();
    }

    private void markFailed(EmailDelivery delivery, String error) {
        delivery.setStatus(EmailDelivery.DeliveryStatus.FAILED);
        delivery.setErrorMessage(error);
    }

    private String buildTestBody(EmailMailboxConfig config) {
        return "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;padding:24px'>"
                + "<h2 style='color:#1a56db'>Prueba de envío — Control Tower</h2>"
                + "<p>Este correo confirma que tu configuración SMTP funciona correctamente.</p>"
                + "<table style='border-collapse:collapse;width:100%;margin-top:16px'>"
                + row("Buzón", config.getName())
                + row("Remitente", config.getFromEmail())
                + row("SMTP", config.getSmtpHost() + ":" + config.getSmtpPort())
                + "</table>"
                + "<p style='margin-top:24px;color:#6b7280;font-size:12px'>Control Tower · Enviado automáticamente</p>"
                + "</div>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:8px;border:1px solid #e5e7eb;font-weight:600;background:#f9fafb'>" + label + "</td>"
                + "<td style='padding:8px;border:1px solid #e5e7eb'>" + value + "</td></tr>";
    }
}
