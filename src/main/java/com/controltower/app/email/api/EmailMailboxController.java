package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.MailboxRequest;
import com.controltower.app.email.api.dto.MailboxResponse;
import com.controltower.app.email.api.dto.TestSendRequest;
import com.controltower.app.email.api.dto.DeliveryResponse;
import com.controltower.app.email.application.EmailOutboundService;
import com.controltower.app.email.application.ImapFetcherService;
import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.email.domain.EmailMailboxConfigRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Email — Mailboxes", description = "IMAP/SMTP mailbox configuration per tenant")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email/mailboxes")
@RequiredArgsConstructor
public class EmailMailboxController {

    private final EmailMailboxConfigRepository mailboxRepo;
    private final AesEncryptor aesEncryptor;
    private final ImapFetcherService imapFetcher;
    private final EmailOutboundService outboundService;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "List all configured mailboxes")
    public ResponseEntity<ApiResponse<List<MailboxResponse>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<MailboxResponse> data = mailboxRepo.findByTenantIdAndActiveTrue(tenantId)
            .stream().map(MailboxResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Add a new IMAP/SMTP mailbox")
    public ResponseEntity<ApiResponse<MailboxResponse>> create(@Valid @RequestBody MailboxRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailMailboxConfig config = mapToEntity(req, new EmailMailboxConfig());
        config.setTenantId(tenantId);
        config.setImapPassword(aesEncryptor.encrypt(req.imapPassword()));
        config.setSmtpPassword(aesEncryptor.encrypt(req.smtpPassword()));
        mailboxRepo.save(config);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Mailbox created", MailboxResponse.from(config)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "Get mailbox details")
    public ResponseEntity<ApiResponse<MailboxResponse>> get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailMailboxConfig config = mailboxRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
        return ResponseEntity.ok(ApiResponse.ok(MailboxResponse.from(config)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Update mailbox configuration")
    public ResponseEntity<ApiResponse<MailboxResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MailboxRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailMailboxConfig config = mailboxRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
        mapToEntity(req, config);
        // Only re-encrypt passwords if they were actually changed (not placeholder)
        if (!req.imapPassword().isBlank() && !req.imapPassword().startsWith("***")) {
            config.setImapPassword(aesEncryptor.encrypt(req.imapPassword()));
        }
        if (!req.smtpPassword().isBlank() && !req.smtpPassword().startsWith("***")) {
            config.setSmtpPassword(aesEncryptor.encrypt(req.smtpPassword()));
        }
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox updated", MailboxResponse.from(config)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Deactivate a mailbox")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailMailboxConfig config = mailboxRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
        config.setActive(false);
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox deactivated"));
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Test IMAP connection for a mailbox")
    public ResponseEntity<ApiResponse<String>> testConnection(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailMailboxConfig config = mailboxRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
        try {
            imapFetcher.fetchUnseen(config);
            return ResponseEntity.ok(ApiResponse.ok("IMAP connection successful"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Connection failed: " + e.getMessage()));
        }
    }

    @PostMapping("/test-send")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Send a test email via a configured mailbox")
    public ResponseEntity<ApiResponse<DeliveryResponse>> testSend(@Valid @RequestBody TestSendRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        DeliveryResponse delivery = DeliveryResponse.from(
            outboundService.sendTest(tenantId, req.mailboxId(), req.to(),
                req.subject() != null ? req.subject() : "Test de Control Tower",
                req.bodyHtml())
        );
        return ResponseEntity.ok(ApiResponse.ok("Test email queued", delivery));
    }

    // ── Mapping helper ────────────────────────────────────────────────────────

    private EmailMailboxConfig mapToEntity(MailboxRequest req, EmailMailboxConfig config) {
        config.setName(req.name());
        config.setImapHost(req.imapHost());
        config.setImapPort(req.imapPort());
        config.setImapSsl(req.imapSsl());
        config.setImapUsername(req.imapUsername());
        config.setImapFolder(req.imapFolder() != null ? req.imapFolder() : "INBOX");
        config.setSmtpHost(req.smtpHost());
        config.setSmtpPort(req.smtpPort());
        config.setSmtpSsl(req.smtpSsl());
        config.setSmtpUsername(req.smtpUsername());
        config.setFromEmail(req.fromEmail());
        config.setFromName(req.fromName());
        config.setPollIntervalSec(req.pollIntervalSec() != null ? req.pollIntervalSec() : 120);
        config.setDepartmentId(req.departmentId());
        return config;
    }
}
