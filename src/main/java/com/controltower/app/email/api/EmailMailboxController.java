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
        if (req.dkimPrivateKey() != null && !req.dkimPrivateKey().isBlank()) {
            config.setDkimPrivateKey(aesEncryptor.encrypt(req.dkimPrivateKey()));
        }
        mailboxRepo.save(config);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Mailbox created", MailboxResponse.from(config)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "Get mailbox details")
    public ResponseEntity<ApiResponse<MailboxResponse>> get(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
        return ResponseEntity.ok(ApiResponse.ok(MailboxResponse.from(config)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Update mailbox configuration")
    public ResponseEntity<ApiResponse<MailboxResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MailboxRequest req) {
        EmailMailboxConfig config = resolve(id);
        mapToEntity(req, config);
        if (!req.imapPassword().isBlank() && !req.imapPassword().startsWith("***")) {
            config.setImapPassword(aesEncryptor.encrypt(req.imapPassword()));
        }
        if (!req.smtpPassword().isBlank() && !req.smtpPassword().startsWith("***")) {
            config.setSmtpPassword(aesEncryptor.encrypt(req.smtpPassword()));
        }
        if (req.dkimPrivateKey() != null && !req.dkimPrivateKey().isBlank()
                && !req.dkimPrivateKey().startsWith("***")) {
            config.setDkimPrivateKey(aesEncryptor.encrypt(req.dkimPrivateKey()));
        } else if (req.dkimSelector() == null || req.dkimSelector().isBlank()) {
            // selector cleared → remove key too
            config.setDkimPrivateKey(null);
        }
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox updated", MailboxResponse.from(config)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Deactivate a mailbox")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
        config.setActive(false);
        mailboxRepo.save(config);
        return ResponseEntity.ok(ApiResponse.ok("Mailbox deactivated"));
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Test IMAP connection")
    public ResponseEntity<ApiResponse<String>> testConnection(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
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
        EmailDelivery delivery = outboundService.sendTest(tenantId, req.mailboxId(), req.to(),
                req.subject() != null ? req.subject() : "Test desde Control Tower",
                req.bodyHtml());

        if (delivery.getStatus() == EmailDelivery.DeliveryStatus.FAILED) {
            String err = delivery.getErrorMessage() != null ? delivery.getErrorMessage() : "SMTP_UNKNOWN_ERROR";
            throw new ControlTowerException(err, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return ResponseEntity.ok(ApiResponse.ok("Test email sent", DeliveryResponse.from(delivery)));
    }

    @GetMapping("/{id}/deliverability")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Check email deliverability — runs DNS lookups for SPF, DKIM, DMARC + SMTP test")
    public ResponseEntity<ApiResponse<DeliverabilityReport>> checkDeliverability(@PathVariable UUID id) {
        EmailMailboxConfig config = resolve(id);
        List<DeliverabilityReport.Check> checks = new ArrayList<>();
        int score = 0;
        String fromDomain = domainOf(config.getFromEmail());
        String authDomain  = domainOf(config.getSmtpUsername());

        // ── 1. SMTP connection (25 pts) ───────────────────────────────────────
        try {
            testSmtpConnection(config);
            checks.add(ok("smtp_connection", "Conexión SMTP exitosa (" + config.getSmtpHost() + ":" + config.getSmtpPort() + ")"));
            score += 25;
        } catch (Exception e) {
            String code = outboundService.classifySmtpError(e);
            checks.add(fail("smtp_connection",
                    "Error de conexión SMTP: " + code,
                    "Verifica host, puerto, usuario y contraseña SMTP."));
        }

        // ── 2. From == SMTP auth (15 pts) ─────────────────────────────────────
        if (config.getFromEmail().equalsIgnoreCase(config.getSmtpUsername())) {
            checks.add(ok("from_auth_match",
                    "Remitente y usuario SMTP son iguales (" + config.getFromEmail() + ") — ideal"));
            score += 15;
        } else if (fromDomain.equalsIgnoreCase(authDomain)) {
            checks.add(warn("from_auth_match",
                    "Remitente (" + config.getFromEmail() + ") y usuario SMTP (" + config.getSmtpUsername() + ") son distintos pero del mismo dominio",
                    "Por máxima compatibilidad con Hotmail/Outlook usa el mismo correo como remitente y usuario SMTP. "
                    + "Algunos servidores rechazan silenciosamente si no coinciden."));
            score += 8;
        } else {
            checks.add(fail("from_auth_match",
                    "Dominio del remitente (" + fromDomain + ") ≠ dominio SMTP (" + authDomain + ")",
                    "Hotmail/Outlook rechazan correos donde el From y el usuario autenticado son de dominios distintos. "
                    + "Usa un remitente del mismo dominio que tu usuario SMTP."));
        }

        // ── 3. SPF — real DNS lookup (15 pts) ────────────────────────────────
        DnsLookupService.SpfResult spf = dnsLookup.lookupSpf(fromDomain);
        if (spf.found()) {
            checks.add(ok("spf",
                    "SPF encontrado en DNS para " + fromDomain + ": " + spf.record()));
            score += 15;
        } else {
            checks.add(fail("spf",
                    "No se encontró registro SPF para " + fromDomain,
                    "Agrega en tu panel DNS (hospedando / cPanel): " + fromDomain
                    + " TXT \"v=spf1 include:" + config.getSmtpHost() + " ~all\""));
        }

        // ── 4. DKIM — real DNS lookup, smart detection (30 pts) ──────────────
        // First check if the app has its own DKIM key configured
        boolean appDkim = config.getDkimSelector() != null && config.getDkimPrivateKey() != null;

        // Then check DNS for both the configured selector and common hosting selectors
        java.util.Optional<DnsLookupService.DkimResult> dnsAppDkim = appDkim
                ? dnsLookup.lookupDkim(fromDomain, config.getDkimSelector())
                : java.util.Optional.empty();
        java.util.Optional<DnsLookupService.DkimResult> dnsHostingDkim = dnsLookup.findAnyDkim(fromDomain);

        if (appDkim && dnsAppDkim.isPresent()) {
            // Best case: app key is configured AND published in DNS
            checks.add(ok("dkim",
                    "DKIM activo — clave del app configurada y publicada en DNS "
                    + "(selector: " + config.getDkimSelector() + "._domainkey." + fromDomain + "). "
                    + "Los correos de Control Tower saldrán firmados."));
            score += 30;
        } else if (appDkim && !dnsAppDkim.isPresent()) {
            // App key configured but not in DNS yet
            checks.add(warn("dkim",
                    "DKIM configurado en el app (selector: " + config.getDkimSelector()
                    + ") pero el registro DNS aún no existe o no se propagó",
                    "Agrega en tu panel DNS:\n"
                    + config.getDkimSelector() + "._domainkey." + fromDomain
                    + " TXT \"v=DKIM1; k=rsa; p=<tu_clave_publica>\"\n"
                    + "La propagación DNS tarda 5–60 min."));
            score += 5;
        } else if (!appDkim && dnsHostingDkim.isPresent()) {
            // No app key, but hosting already signs DKIM → check if it covers SMTP submissions
            DnsLookupService.DkimResult hostingResult = dnsHostingDkim.get();
            checks.add(warn("dkim",
                    "Tu proveedor de hosting tiene DKIM publicado en DNS "
                    + "(selector " + hostingResult.selector() + "._domainkey." + fromDomain + ")",
                    "Si mandas correos manualmente desde el panel de tu hosting y sí llegan a Gmail/Hotmail, "
                    + "es porque el servidor de hosting firma DKIM automáticamente. "
                    + "SIN EMBARGO: muchos proveedores (incluyendo algunos cPanel) "
                    + "SOLO firman en webmail, no en envíos SMTP externos como Control Tower.\n\n"
                    + "OPCIÓN A (sin tocar DNS): Prueba enviar a Gmail/Hotmail y revisa la carpeta Spam. "
                    + "Si llega al spam, el hosting SÍ firma pero hay otro problema. "
                    + "Si no llega ni al spam, el hosting NO firma para apps externas — necesitas DKIM en Control Tower.\n\n"
                    + "OPCIÓN B (recomendada): Configura DKIM en Control Tower (sección DKIM de este buzón) "
                    + "con un selector diferente, ej. 'ct'. Solo necesitas agregar un registro DNS más — "
                    + "el que ya tienes no se toca."));
            score += 15;
        } else {
            // No DKIM anywhere
            checks.add(fail("dkim",
                    "No se encontró DKIM en DNS para " + fromDomain
                    + " (se revisaron " + DnsLookupService.COMMON_SELECTORS.size() + " selectores comunes)",
                    "DKIM es la causa más común de rechazo en Gmail y Hotmail. "
                    + "Si puedes mandar desde el panel del hosting y sí llega, tu hosting probablemente "
                    + "firma en webmail pero no para apps externas.\n\n"
                    + "Configura DKIM en Control Tower: sección DKIM → selector 'ct' → genera claves con OpenSSL → agrega TXT en DNS."));
        }

        // ── 5. DMARC — real DNS lookup (10 pts) ──────────────────────────────
        DnsLookupService.DmarcResult dmarc = dnsLookup.lookupDmarc(fromDomain);
        if (dmarc.found()) {
            String policy = dmarc.policy();
            if ("reject".equals(policy) || "quarantine".equals(policy)) {
                checks.add(ok("dmarc",
                        "DMARC configurado para " + fromDomain + " — política: " + policy.toUpperCase()
                        + ". Los correos que pasen SPF+DKIM se entregan normalmente."));
                score += 10;
            } else {
                checks.add(ok("dmarc",
                        "DMARC encontrado (p=none) — monitoreo activo pero sin rechazo. "
                        + "Es suficiente para deliverability. Registro: " + dmarc.record()));
                score += 8;
            }
        } else {
            checks.add(warn("dmarc",
                    "No se encontró registro DMARC para " + fromDomain,
                    "DMARC no es obligatorio hoy, pero Google y Microsoft ya lo piden para dominios de alto volumen. "
                    + "Si tienes acceso al DNS de " + fromDomain + ", agrégalo:\n"
                    + "_dmarc." + fromDomain + " TXT \"v=DMARC1; p=none; rua=mailto:dmarc@" + fromDomain + "\"\n"
                    + "(p=none = solo monitoreo, no afecta entrega)"));
            score += 5;
        }

        // ── 6. TLS (5 pts) ───────────────────────────────────────────────────
        boolean hasTls = config.getSmtpPort() == 465 || config.isSmtpSsl();
        if (hasTls) {
            checks.add(ok("tls", "TLS/SSL habilitado (puerto " + config.getSmtpPort() + ")"));
            score += 5;
        } else {
            checks.add(warn("tls",
                    "Sin TLS — conexión SMTP sin cifrado",
                    "Usa puerto 465 (SMTPS) o 587 con STARTTLS."));
        }

        return ResponseEntity.ok(ApiResponse.ok(new DeliverabilityReport(checks, score, DeliverabilityReport.verdict(score))));
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
        config.setDkimSelector(req.dkimSelector());
        return config;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        // createMimeMessage() opens a connection to verify credentials
        MimeMessage msg = sender.createMimeMessage();
        new MimeMessageHelper(msg, false).setTo("test@example.com");
    }

    /** Green: everything correct, no action needed. */
    private static DeliverabilityReport.Check ok(String name, String message) {
        return new DeliverabilityReport.Check(name, true, message, null);
    }

    /** Yellow: partial — scores points but has an advisory action. */
    private static DeliverabilityReport.Check warn(String name, String message, String action) {
        return new DeliverabilityReport.Check(name, true, message, action);
    }

    /** Red: missing or broken — action is required. */
    private static DeliverabilityReport.Check fail(String name, String message, String action) {
        return new DeliverabilityReport.Check(name, false, message, action);
    }

    private static String domainOf(String email) {
        int at = email != null ? email.lastIndexOf('@') : -1;
        return at >= 0 ? email.substring(at + 1).toLowerCase() : "";
    }
}
