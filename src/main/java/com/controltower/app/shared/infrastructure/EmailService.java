package com.controltower.app.shared.infrastructure;

import com.controltower.app.email.application.EmailOutboundService;
import com.controltower.app.tenancy.domain.TenantConfigRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around JavaMailSender.
 * Every send first tries the tenant's own configured mailbox (Settings → Email,
 * see {@link EmailOutboundService}) so deliverability matches what the tenant set
 * up (SPF/DKIM/from-domain). Only when no active mailbox exists does it fall back
 * to the global spring.mail.* SMTP account; if that isn't configured either, it
 * logs a warning and silently skips sending.
 * The From address for the fallback path resolves per-tenant via TenantConfig key
 * "mail.from", falling back to the global spring.mail.from property.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private TenantConfigRepository tenantConfigRepository;

    @Autowired(required = false)
    private EmailOutboundService emailOutboundService;

    @Value("${spring.mail.from:noreply@controltower.io}")
    private String defaultFrom;

    private String resolveFrom() {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null && tenantConfigRepository != null) {
                return tenantConfigRepository.findByTenantIdAndKey(tenantId, "mail.from")
                        .map(cfg -> cfg.getValue())
                        .filter(v -> v != null && !v.isBlank())
                        .orElse(defaultFrom);
            }
        } catch (Exception ignored) {}
        return defaultFrom;
    }

    /**
     * Tries to queue the email through the tenant's own mailbox.
     * @return true if a mailbox was found (the caller should not fall back to the
     *         global SMTP account); false if there's no mailbox configured for the
     *         current tenant, or no tenant context is available at all.
     */
    private boolean sendViaTenantMailbox(String toEmail, String subject, String plainBody) {
        if (emailOutboundService == null) return false;
        UUID tenantId;
        try {
            tenantId = TenantContext.getTenantId();
        } catch (Exception ignored) {
            return false;
        }
        if (tenantId == null) return false;

        String html = "<div style=\"font-family:sans-serif;white-space:pre-wrap\">"
                + escapeHtml(plainBody) + "</div>";
        try {
            return emailOutboundService.trySendNotification(tenantId, toEmail, subject, html);
        } catch (Exception e) {
            log.warn("Tenant mailbox send failed for {}, falling back to global SMTP: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public void sendPasswordReset(String toEmail, String resetLink) {
        String subject = "Reset your Control Tower password";
        String body = "Click the link below to reset your password (expires in 1 hour):\n\n"
                + resetLink + "\n\nIf you did not request a password reset, ignore this email.";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Password reset email queued via tenant mailbox for {}", toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendWelcome(String toEmail, String fullName) {
        String subject = "Welcome to Control Tower!";
        String body = "Hi " + fullName + ",\n\nYour Control Tower account has been created. "
                + "You can now log in and start managing your operations.\n\nWelcome aboard!";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Welcome email queued via tenant mailbox for {}", toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Welcome email for {} ({})", toEmail, fullName);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send welcome email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendProposal(String toEmail, String clientName, String proposalNumber,
                              String proposalTitle, String total, String validUntil,
                              String proposalUrl) {
        String subject = "Propuesta Económica " + proposalNumber + " — " + proposalTitle;
        String body =
                "Estimado/a " + clientName + ",\n\n"
                + "Le enviamos la propuesta económica " + proposalNumber + ":\n\n"
                + "Título: " + proposalTitle + "\n"
                + "Total: " + total + "\n"
                + "Válida hasta: " + validUntil + "\n\n"
                + (proposalUrl != null && !proposalUrl.isBlank()
                        ? "Ver propuesta: " + proposalUrl + "\n\n" : "")
                + "Para aceptar o rechazar esta propuesta, por favor contáctenos.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Proposal email {} queued via tenant mailbox for {}", proposalNumber, toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Proposal {} for {}", proposalNumber, toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Proposal email {} sent to {}", proposalNumber, toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send proposal email {} to {}: {}", proposalNumber, toEmail, ex.getMessage());
        }
    }

    public void sendFinanceReport(String toEmail, String from, String to,
                                   String grandTotal, List<String> summaryLines) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Reporte de Gastos\n");
        bodyBuilder.append("Período: ").append(from).append(" — ").append(to).append("\n");
        bodyBuilder.append("Total: ").append(grandTotal).append("\n\n");
        bodyBuilder.append("Desglose por categoría:\n");
        if (summaryLines != null) {
            summaryLines.forEach(line -> bodyBuilder.append("  ").append(line).append("\n"));
        }
        bodyBuilder.append("\nGenerado automáticamente por Control Tower.");
        String subject = "Reporte de Gastos — " + from + " al " + to;
        String body = bodyBuilder.toString();

        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Finance report email queued via tenant mailbox for {}", toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Finance report to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Finance report email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send finance report to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendTicketCommentNotification(String toEmail, String ticketTitle,
                                               String agentName, String commentContent) {
        String subject = "Nueva respuesta en tu ticket: " + ticketTitle;
        String body =
                "Hola,\n\n"
                + agentName + " ha respondido en tu ticket \"" + ticketTitle + "\":\n\n"
                + "---\n" + commentContent + "\n---\n\n"
                + "Si tienes alguna duda, puedes responder directamente a este correo o acceder al portal.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Ticket comment notification queued via tenant mailbox for {} (ticket '{}')", toEmail, ticketTitle);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Ticket comment notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Ticket comment notification sent to {} for ticket '{}'", toEmail, ticketTitle);
        } catch (MailException ex) {
            log.warn("Failed to send ticket comment notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendChatReplyNotification(String toEmail, String visitorName,
                                           String agentName, String messageContent) {
        String subject = "Tienes un nuevo mensaje de soporte";
        String body =
                "Hola " + visitorName + ",\n\n"
                + agentName + " te ha enviado un mensaje en el chat de soporte:\n\n"
                + "---\n" + messageContent + "\n---\n\n"
                + "Ingresa al chat para continuar la conversación.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Chat reply notification queued via tenant mailbox for {}", toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Chat reply notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Chat reply notification sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send chat reply notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendKanbanCardNotification(String toEmail, String assigneeName,
                                            String cardTitle, String movedBy,
                                            String fromColumn, String toColumn) {
        String subject = "Actualización en tarjeta: " + cardTitle;
        String body =
                "Hola " + assigneeName + ",\n\n"
                + movedBy + " movió la tarjeta \"" + cardTitle + "\":\n\n"
                + "  De: " + fromColumn + "\n"
                + "  A:  " + toColumn + "\n\n"
                + "Ingresa a Control Tower para ver los detalles.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Kanban card notification queued via tenant mailbox for {} (card '{}')", toEmail, cardTitle);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Kanban card notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Kanban card notification sent to {} for card '{}'", toEmail, cardTitle);
        } catch (MailException ex) {
            log.warn("Failed to send kanban card notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendNoteNotification(String toEmail, String recipientName,
                                      String authorName, String noteTitle, String noteContent,
                                      String linkedTo, boolean isReply) {
        String action = isReply ? "respondió una nota interna" : "escribió una nota interna";
        String context = linkedTo != null ? " en " + linkedTo.toLowerCase().replace("_", " ") : "";
        String preview = noteContent != null && !noteContent.isBlank()
                ? "\n\n" + noteContent.substring(0, Math.min(noteContent.length(), 300))
                : "";
        String subject = (isReply ? "[Respuesta] " : "[Nota] ") + noteTitle;
        String body =
                "Hola " + recipientName + ",\n\n"
                + authorName + " " + action + context + ":\n\n"
                + "  Asunto: " + noteTitle + preview + "\n\n"
                + "Ingresa a Control Tower para ver y responder.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Note notification queued via tenant mailbox for {} (isReply={})", toEmail, isReply);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Note notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Note notification sent to {} (isReply={})", toEmail, isReply);
        } catch (MailException ex) {
            log.warn("Failed to send note notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendHealthIncidentNotification(String toEmail, String recipientName,
                                                String branchName, String severity, String description) {
        String subject = "[" + severity + "] Incidente en " + branchName;
        String body =
                "Hola " + recipientName + ",\n\n"
                + "Se detectó un incidente de salud en \"" + branchName + "\":\n\n"
                + "  Severidad: " + severity + "\n"
                + "  Descripción: " + description + "\n\n"
                + "Ingresa a Control Tower para ver los detalles y darle seguimiento.\n\n"
                + "Atentamente,\nControl Tower";
        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Health incident notification queued via tenant mailbox for {} (branch '{}')", toEmail, branchName);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Health incident notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Health incident notification sent to {} for branch '{}'", toEmail, branchName);
        } catch (MailException ex) {
            log.warn("Failed to send health incident notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendReciboNomina(String toEmail, String employeeName, String periodLabel,
                                  String grossPay, String imssEmployee, String isr,
                                  String infonavit, String otherDeductions, String netPay,
                                  String currency) {
        String subject = "Recibo de Nómina — " + periodLabel;
        String body = "Recibo de Nómina\n" +
            "Empleado: " + employeeName + "\n" +
            "Período: " + periodLabel + "\n\n" +
            "Percepciones:\n" +
            "  Sueldo bruto: " + grossPay + " " + currency + "\n\n" +
            "Deducciones:\n" +
            "  IMSS (empleado): " + imssEmployee + " " + currency + "\n" +
            "  ISR: " + isr + " " + currency + "\n" +
            "  INFONAVIT: " + infonavit + " " + currency + "\n" +
            "  Otras deducciones: " + otherDeductions + " " + currency + "\n\n" +
            "Neto a pagar: " + netPay + " " + currency + "\n\n" +
            "Generado automáticamente por Control Tower.";

        if (sendViaTenantMailbox(toEmail, subject, body)) {
            log.info("Payroll receipt queued via tenant mailbox for {}", toEmail);
            return;
        }
        if (mailSender == null) {
            log.warn("Mail sender not configured. Payroll receipt to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Payroll receipt sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send payroll receipt to {}: {}", toEmail, ex.getMessage());
        }
    }
}
