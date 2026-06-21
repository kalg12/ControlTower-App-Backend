package com.controltower.app.email.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "email_raw")
@Getter
@Setter
public class EmailRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "mailbox_id", nullable = false)
    private UUID mailboxId;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    @Array(length = 50)
    @Column(name = "references_ids", columnDefinition = "TEXT[]")
    private String[] referencesIds;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "from_name")
    private String fromName;

    @Array(length = 20)
    @Column(name = "to_email", columnDefinition = "TEXT[]", nullable = false)
    private String[] toEmail;

    @Array(length = 20)
    @Column(name = "cc_email", columnDefinition = "TEXT[]")
    private String[] ccEmail;

    @Column(name = "reply_to")
    private String replyTo;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "JSONB")
    private Map<String, Object> headers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "JSONB")
    private List<Map<String, Object>> attachments;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status = EmailStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "spam_score")
    private Double spamScore;

    @Column(name = "is_spam", nullable = false)
    private boolean spam = false;

    @Column(name = "alias_id")
    private UUID aliasId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum EmailStatus {
        RECEIVED, PROCESSING, PROCESSED, FAILED, DUPLICATE, SPAM
    }
}
