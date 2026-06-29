package com.controltower.app.email.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_mailbox_configs")
@Getter
@Setter
public class EmailMailboxConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    // ── IMAP ──────────────────────────────────────────────────────────────────
    @Column(name = "imap_host", nullable = false)
    private String imapHost;

    @Column(name = "imap_port", nullable = false)
    private int imapPort = 993;

    @Column(name = "imap_ssl", nullable = false)
    private boolean imapSsl = true;

    @Column(name = "imap_username", nullable = false)
    private String imapUsername;

    /** Stored AES-256-GCM encrypted via AesEncryptor. */
    @Column(name = "imap_password", nullable = false)
    private String imapPassword;

    @Column(name = "imap_folder", nullable = false)
    private String imapFolder = "INBOX";

    // ── SMTP ──────────────────────────────────────────────────────────────────
    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private int smtpPort = 587;

    @Column(name = "smtp_ssl", nullable = false)
    private boolean smtpSsl = true;

    @Column(name = "smtp_username", nullable = false)
    private String smtpUsername;

    /** Stored AES-256-GCM encrypted via AesEncryptor. */
    @Column(name = "smtp_password", nullable = false)
    private String smtpPassword;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "from_name")
    private String fromName;

    // ── Control ───────────────────────────────────────────────────────────────
    @Column(name = "poll_interval_sec", nullable = false)
    private int pollIntervalSec = 120;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;


    public void incrementErrorCount() {
        this.errorCount++;
    }

    public boolean isDueForPoll(Instant now) {
        if (lastPolledAt == null) return true;
        return lastPolledAt.plusSeconds(pollIntervalSec).isBefore(now);
    }
}
