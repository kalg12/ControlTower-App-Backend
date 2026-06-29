package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.DeliverabilityReport;
import com.controltower.app.email.api.dto.DeliveryResponse;
import com.controltower.app.email.api.dto.MailboxRequest;
import com.controltower.app.email.api.dto.MailboxResponse;
import com.controltower.app.email.api.dto.TestSendRequest;
import com.controltower.app.email.application.DnsLookupService;
import com.controltower.app.email.application.EmailOutboundService;
import com.controltower.app.email.application.ImapFetcherService;
import com.controltower.app.email.domain.EmailDelivery;
import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.email.domain.EmailMailboxConfigRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
    private final DnsLookupService dnsLookup;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    public ResponseEntity<ApiResponse<List<MailboxResponse>>> list() {
        List<MailboxResponse> data = mailboxRepo.findByTenantIdAndActiveTrue(TenantContext.getTenantId())
                .stream().map(MailboxResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<MailboxResponse>> create(@Valid @RequestBody MailboxRequest req) {
        EmailMailboxConfig config = mapToEntity(req, new EmailMailboxConfig());
        config.setTenantId(TenantContext.getTenantId());
        config.setImapPassword(aesEncryptor.encrypt(req.imapPassword()));
        config.setSmtpPassword(aesEncryptor.encrypt(req.smtpPassword()));
        mailboxRepo.save(config);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Mailbox created", MailboxResponse.from(config)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('email:read')")
    public ResponseEntity<ApiResponse<MailboxResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(MailboxResponse.from(resolve(id))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<MailboxResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody MailboxRequest req) {
        EmailMailboxConfig config = resolve(id);
        mapToEntity(req, config);
        if (!req.imapPassword().isBlank() && !req.imapPassword().startsWith("***"))
            config.setImapPassword(aesEncryptor.encrypt(req.imapPassword()));
        if (!req.smtpPassword().isBlank() && !req.smtpPassword().startsWith("***"))
            config.setSmtpPassword(aesEncryptor.encrypt(req.smtpPassword()));
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox updated", MailboxResponse.from(config)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
        config.setActive(false);
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox deactivated"));
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<String>> testConnection(@PathVariable UUID id) {
        try {
            imapFetcher.fetchUnseen(resolve(id));
            return ResponseEntity.ok(ApiResponse.ok("IMAP connection successful"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Connection failed: " + e.getMessage()));
        }
    }

    @PostMapping("/test-send")
    @PreAuthorize("hasAuthority('email:manage')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> testSend(@Valid @RequestBody TestSendRequest req) {
        EmailDelivery delivery = outboundService.sendTest(TenantContext.getTenantId(), req.mailboxId(), req.to(),
                req.subject() != null ? req.subject() : "Test desde Control Tower", req.bodyHtml());

        if (delivery.getStatus() == EmailDelivery.DeliveryStatus.FAILED) {
            String err = delivery.getErrorMessage() != null ? delivery.getErrorMessage() : "SMTP_UNKNOWN_ERROR";
            throw new ControlTowerException(err, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(ApiResponse.ok("Test email sent", DeliveryResponse.from(delivery)));
    }

    @GetMapping("/{id}/deliverability")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Check deliverability: SMTP + SPF + DKIM (DNS) + DMARC")
    public ResponseEntity<ApiResponse<DeliverabilityReport>> checkDeliverability(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
        List<DeliverabilityReport.Check> checks = new ArrayList<>();
        int score = 0;
        String fromDomain = domainOf(config.getFromEmail());
        String authDomain  = domainOf(config.getSmtpUsername());

        // 1. SMTP connection (30 pts)
        try {
            testSmtpConnection(config);
            checks.add(ok("smtp_connection", "Conexión SMTP exitosa (" + config.getSmtpHost() + ":" + config.getSmtpPort() + ")"));
            score += 30;
        } catch (Exception e) {
            checks.add(fail("smtp_connection",
                    "Error de conexión SMTP: " + outboundService.classifySmtpError(e),
                    "Verifica host, puerto, usuario y contraseña SMTP."));
        }

        // 2. From == SMTP auth (20 pts)
        if (config.getFromEmail().equalsIgnoreCase(config.getSmtpUsername())) {
            checks.add(ok("from_auth_match", "Remitente y usuario SMTP son iguales (" + config.getFromEmail() + ")"));
            score += 20;
        } else if (fromDomain.equalsIgnoreCase(authDomain)) {
            checks.add(warn("from_auth_match",
                    "Remitente (" + config.getFromEmail() + ") y usuario SMTP (" + config.getSmtpUsername() + ") son distintos pero del mismo dominio",
                    "Para máxima compatibilidad con Hotmail/Outlook usa el mismo correo como remitente y usuario SMTP."));
            score += 10;
        } else {
            checks.add(fail("from_auth_match",
                    "Dominio del remitente (" + fromDomain + ") ≠ dominio del usuario SMTP (" + authDomain + ")",
                    "Hotmail/Outlook rechazan correos donde From y el usuario autenticado son de dominios distintos."));
        }

        // 3. SPF — real DNS lookup (20 pts)
        DnsLookupService.SpfResult spf = dnsLookup.lookupSpf(fromDomain);
        if (spf.found()) {
            checks.add(ok("spf", "SPF encontrado: " + spf.record()));
            score += 20;
        } else {
            checks.add(fail("spf",
                    "No se encontró registro SPF para " + fromDomain,
                    "En tu panel de hosting (cPanel → Email Deliverability) activa SPF con un clic. "
                    + "O agrega manualmente: " + fromDomain + " TXT \"v=spf1 include:" + config.getSmtpHost() + " ~all\""));
        }

        // 4. DKIM — informational DNS lookup only (20 pts)
        java.util.Optional<DnsLookupService.DkimResult> dnsHostingDkim = dnsLookup.findAnyDkim(fromDomain);
        if (dnsHostingDkim.isPresent()) {
            DnsLookupService.DkimResult r = dnsHostingDkim.get();
            checks.add(ok("dkim",
                    "DKIM encontrado en DNS (selector: " + r.selector() + "._domainkey." + fromDomain + "). "
                    + "Tu hosting firma los correos automáticamente."));
            score += 20;
        } else {
            checks.add(warn("dkim",
                    "No se encontró DKIM en DNS para " + fromDomain,
                    "Ve a tu panel de hosting → Email → Email Deliverability → Reparar DKIM. "
                    + "Tu hosting probablemente tiene la opción pero no está activada."));
        }

        // 5. DMARC — real DNS lookup (20 pts)
        DnsLookupService.DmarcResult dmarc = dnsLookup.lookupDmarc(fromDomain);
        if (dmarc.found()) {
            checks.add(ok("dmarc", "DMARC configurado (p=" + dmarc.policy() + "): " + dmarc.record()));
            score += 20;
        } else {
            checks.add(warn("dmarc",
                    "No se encontró DMARC para " + fromDomain + " — no es bloqueante hoy pero Google/Microsoft lo recomiendan",
                    "En cPanel → Email Deliverability actívalo con un clic. "
                    + "O agrega: _dmarc." + fromDomain + " TXT \"v=DMARC1; p=none; rua=mailto:dmarc@" + fromDomain + "\""));
        }

        // 6. TLS (10 pts)
        if (config.getSmtpPort() == 465 || config.isSmtpSsl()) {
            checks.add(ok("tls", "TLS/SSL habilitado (puerto " + config.getSmtpPort() + ")"));
            score += 10;
        } else {
            checks.add(warn("tls", "Sin TLS — conexión SMTP no cifrada", "Usa puerto 465 (SMTPS) o 587 con STARTTLS."));
        }

        return ResponseEntity.ok(ApiResponse.ok(new DeliverabilityReport(checks, score, DeliverabilityReport.verdict(score))));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private EmailMailboxConfig resolve(UUID id) {
        return mailboxRepo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Mailbox not found"));
    }

    private void testSmtpConnection(EmailMailboxConfig config) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setUsername(config.getSmtpUsername());
        sender.setPassword(aesEncryptor.decrypt(config.getSmtpPassword()));
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        if (config.getSmtpPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else if (config.isSmtpSsl()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        MimeMessage msg = sender.createMimeMessage();
        new MimeMessageHelper(msg, false).setTo("test@example.com");
    }

    private static DeliverabilityReport.Check ok(String name, String message) {
        return new DeliverabilityReport.Check(name, true, message, null);
    }

    private static DeliverabilityReport.Check warn(String name, String message, String action) {
        return new DeliverabilityReport.Check(name, true, message, action);
    }

    private static DeliverabilityReport.Check fail(String name, String message, String action) {
        return new DeliverabilityReport.Check(name, false, message, action);
    }

    private static String domainOf(String email) {
        int at = email != null ? email.lastIndexOf('@') : -1;
        return at >= 0 ? email.substring(at + 1).toLowerCase() : "";
    }
}
