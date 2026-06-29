package com.controltower.app.email.api.dto;

import com.controltower.app.email.domain.EmailMailboxConfig;

import java.time.Instant;
import java.util.UUID;

/** Passwords and private keys are never returned — only metadata. */
public record MailboxResponse(
    UUID id,
    UUID tenantId,
    String name,
    String imapHost,
    int imapPort,
    boolean imapSsl,
    String imapUsername,
    String imapFolder,
    String smtpHost,
    int smtpPort,
    boolean smtpSsl,
    String smtpUsername,
    String fromEmail,
    String fromName,
    int pollIntervalSec,
    Instant lastPolledAt,
    String lastError,
    int errorCount,
    UUID departmentId,
    boolean active,
    Instant createdAt,
    // DKIM — selector only; private key is never returned
    String dkimSelector,
    boolean dkimConfigured
) {
    public static MailboxResponse from(EmailMailboxConfig m) {
        return new MailboxResponse(
            m.getId(), m.getTenantId(), m.getName(),
            m.getImapHost(), m.getImapPort(), m.isImapSsl(), m.getImapUsername(), m.getImapFolder(),
            m.getSmtpHost(), m.getSmtpPort(), m.isSmtpSsl(), m.getSmtpUsername(),
            m.getFromEmail(), m.getFromName(), m.getPollIntervalSec(),
            m.getLastPolledAt(), m.getLastError(), m.getErrorCount(),
            m.getDepartmentId(), m.isActive(), m.getCreatedAt(),
            m.getDkimSelector(),
            m.getDkimSelector() != null && m.getDkimPrivateKey() != null
        );
    }
}
