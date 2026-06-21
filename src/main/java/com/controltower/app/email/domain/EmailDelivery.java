package com.controltower.app.email.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_deliveries")
@Getter
@Setter
public class EmailDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "mailbox_id")
    private UUID mailboxId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Array(length = 20)
    @Column(name = "to_email", columnDefinition = "TEXT[]", nullable = false)
    private String[] toEmail;

    @Array(length = 20)
    @Column(name = "cc_email", columnDefinition = "TEXT[]")
    private String[] ccEmail;

    @Array(length = 20)
    @Column(name = "bcc_email", columnDefinition = "TEXT[]")
    private String[] bccEmail;

    @Column(name = "reply_to")
    private String replyTo;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status = DeliveryStatus.QUEUED;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType = DeliveryType.TICKET_REPLY;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum DeliveryStatus {
        QUEUED, SENT, FAILED
    }

    public enum DeliveryType {
        TICKET_REPLY, AUTO_REPLY, NOTIFICATION, TEST
    }
}
