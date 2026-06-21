package com.controltower.app.email.application;

import com.controltower.app.email.domain.EmailDelivery;
import com.controltower.app.email.domain.EmailDeliveryRepository;
import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.email.domain.EmailMailboxConfigRepository;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        EmailDelivery delivery = new EmailDelivery();
        delivery.setTenantId(tenantId);
        delivery.setMailboxId(mailboxId);
        delivery.setToEmail(new String[]{toEmail});
        delivery.setSubject(subject);
        delivery.setBodyHtml(bodyHtml != null ? bodyHtml : "<p>Test email from Control Tower</p>");
        delivery.setDeliveryType(EmailDelivery.DeliveryType.TEST);
        delivery.setStatus(EmailDelivery.DeliveryStatus.QUEUED);
        deliveryRepo.save(delivery);

        attempt(delivery);
        return delivery;
    }

    /** Called by EmailRetryScheduler for QUEUED deliveries that have failed before. */
    public void attempt(EmailDelivery delivery) {
        EmailMailboxConfig config = mailboxRepo.findById(delivery.getMailboxId()).orElse(null);
        if (config == null) {
            markFailed(delivery, "Mailbox config not found: " + delivery.getMailboxId());
            return;
        }

        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastAttemptAt(Instant.now());

        try {
            doSend(delivery, config);
            delivery.setStatus(EmailDelivery.DeliveryStatus.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setErrorMessage(null);
            log.info("Email delivered to {} (delivery {})", delivery.getToEmail()[0], delivery.getId());
        } catch (Exception e) {
            log.warn("Email delivery failed (attempt {}/{}): {}", delivery.getAttempts(), MAX_RETRY_ATTEMPTS, e.getMessage());
            if (delivery.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                markFailed(delivery, e.getMessage());
            } else {
                delivery.setNextRetryAt(Instant.now().plusSeconds(RETRY_BACKOFF_MINUTES * 60L * delivery.getAttempts()));
                delivery.setErrorMessage(e.getMessage());
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
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpSsl()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        return sender;
    }

    private void markFailed(EmailDelivery delivery, String error) {
        delivery.setStatus(EmailDelivery.DeliveryStatus.FAILED);
        delivery.setErrorMessage(error);
    }
}
