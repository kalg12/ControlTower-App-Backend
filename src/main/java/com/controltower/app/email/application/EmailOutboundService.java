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

/**
 * Sends outbound emails via the tenant's configured SMTP mailbox.
 * Creates a dynamic JavaMailSender per mailbox config (multi-tenant).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboundService {

    private final EmailDeliveryRepository deliveryRepo;
    private final EmailMailboxConfigRepository mailboxRepo;
    private final AesEncryptor aesEncryptor;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MINUTES = 10;

    /**
     * Sends an outbound reply for a ticket.
     * Call this when an agent adds a comment and the original ticket has a requesterEmail.
     */
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

    /** Sends a test email — used by the admin panel "test send" endpoint. */
    public EmailDelivery sendTest(UUID tenantId, UUID mailboxId, String toEmail,
                                   String subject, String bodyHtml) {
        EmailMailboxConfig config = mailboxRepo.findById(mailboxId)
                .orElseThrow(() -> new ControlTowerException(
                        "Mailbox not found: " + mailboxId, HttpStatus.NOT_FOUND));

        EmailDelivery delivery = new EmailDelivery();
        delivery.setTenantId(tenantId);
        delivery.setMailboxId(mailboxId);
        delivery.setFromEmail(config.getFromEmail());   // required NOT NULL column
        delivery.setToEmail(new String[]{toEmail});
        delivery.setSubject(subject);
        delivery.setBodyHtml(bodyHtml != null ? bodyHtml : "<p>Test email from Control Tower</p>");
        delivery.setDeliveryType(EmailDelivery.DeliveryType.TEST);
        delivery.setStatus(EmailDelivery.DeliveryStatus.QUEUED);
        deliveryRepo.save(delivery);

        attempt(delivery, config);   // pass already-fetched config to skip second DB lookup
        return delivery;
    }

    /** Called by EmailRetryScheduler for QUEUED deliveries that have failed before. */
    public void attempt(EmailDelivery delivery) {
        EmailMailboxConfig config = mailboxRepo.findById(delivery.getMailboxId()).orElse(null);
        if (config == null) {
            markFailed(delivery, "SMTP_CONFIG_ERROR: Mailbox config not found: " + delivery.getMailboxId());
            deliveryRepo.save(delivery);
            return;
        }
        attempt(delivery, config);
    }

    /** Internal overload used when the config was already fetched (avoids double DB lookup). */
    void attempt(EmailDelivery delivery, EmailMailboxConfig config) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastAttemptAt(Instant.now());

        try {
            doSend(delivery, config);
            delivery.setStatus(EmailDelivery.DeliveryStatus.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setErrorMessage(null);
            log.info("[SMTP] Delivered to {} via {}:{} (delivery {})",
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

        // Reply-To: ticket+{ticketId}@domain for future thread detection
        if (delivery.getTicketId() != null) {
            String domain = config.getFromEmail().substring(config.getFromEmail().indexOf('@') + 1);
            helper.setReplyTo("ticket+" + delivery.getTicketId() + "@" + domain);
        }

        helper.setSubject(delivery.getSubject() != null ? delivery.getSubject() : "(Sin asunto)");
        helper.setText(delivery.getBodyHtml() != null ? delivery.getBodyHtml() : "", true);

        if (delivery.getInReplyTo() != null) {
            message.setHeader("In-Reply-To", delivery.getInReplyTo());
            message.setHeader("References", delivery.getInReplyTo());
        }
        message.setHeader("X-Auto-Submitted", "auto-replied");

        sender.send(message);
    }

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

        if (config.getSmtpPort() == 465) {
            // SMTPS — SSL/TLS from the start (port 465)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else if (config.isSmtpSsl()) {
            // STARTTLS — upgrade after plaintext connect (port 587 or custom)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.enable", "false");
        } else {
            // No encryption (port 25 or custom without SSL)
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
        }

        return sender;
    }

    private String classifySmtpError(Exception e) {
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

    private void markFailed(EmailDelivery delivery, String error) {
        delivery.setStatus(EmailDelivery.DeliveryStatus.FAILED);
        delivery.setErrorMessage(error);
    }
}
