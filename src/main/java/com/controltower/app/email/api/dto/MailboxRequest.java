package com.controltower.app.email.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MailboxRequest(
    @NotBlank String name,

    // IMAP
    @NotBlank String imapHost,
    @NotNull Integer imapPort,
    boolean imapSsl,
    @NotBlank String imapUsername,
    @NotBlank String imapPassword,
    String imapFolder,

    // SMTP
    @NotBlank String smtpHost,
    @NotNull Integer smtpPort,
    boolean smtpSsl,
    @NotBlank String smtpUsername,
    @NotBlank String smtpPassword,
    @NotBlank @Email String fromEmail,
    String fromName,

    // Control
    Integer pollIntervalSec,
    UUID departmentId
) {}
