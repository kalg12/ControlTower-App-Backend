package com.controltower.app.shared.infrastructure;

import com.controltower.app.shared.config.ResendProperties;
import com.controltower.app.tenancy.domain.TenantConfigRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends all system emails through Resend's HTTP API.
 * The From address resolves per-tenant via TenantConfig key "mail.from",
 * falling back to resend.from-email/from-name.
 * If resend.api-key is not set, logs a warning and silently skips sending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final ResendProperties resendProperties;
    private final TenantConfigRepository tenantConfigRepository;

    private boolean isConfigured() {
        return resendProperties.getApiKey() != null && !resendProperties.getApiKey().isBlank();
    }

    private String resolveFrom() {
        String defaultFrom = resendProperties.getFromName() + " <" + resendProperties.getFromEmail() + ">";
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                return tenantConfigRepository.findByTenantIdAndKey(tenantId, "mail.from")
                        .map(cfg -> cfg.getValue())
                        .filter(v -> v != null && !v.isBlank())
                        .orElse(defaultFrom);
            }
        } catch (Exception ignored) {}
        return defaultFrom;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String toHtml(String plainBody) {
        return "<div style=\"font-family:sans-serif;white-space:pre-wrap\">" + escapeHtml(plainBody) + "</div>";
    }

    private void send(String toEmail, String subject, String plainBody, String logContext) {
        if (!isConfigured()) {
            log.warn("Resend not configured (RESEND_API_KEY missing). {} for {}", logContext, toEmail);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendProperties.getApiKey());

            Map<String, Object> payload = Map.of(
                    "from", resolveFrom(),
                    "to", List.of(toEmail),
                    "subject", subject,
                    "html", toHtml(plainBody)
            );

            var response = restTemplate.postForEntity(RESEND_ENDPOINT, new HttpEntity<>(payload, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("{} sent via Resend to {}", logContext, toEmail);
            } else {
                log.warn("Resend returned {} for {} to {}: {}",
                        response.getStatusCode(), logContext, toEmail, response.getBody());
            }
        } catch (RestClientException ex) {
            log.warn("Failed to send {} to {} via Resend: {}", logContext, toEmail, ex.getMessage());
        }
    }

    public void sendPasswordReset(String toEmail, String resetLink) {
        send(toEmail, "Reset your Control Tower password",
                "Click the link below to reset your password (expires in 1 hour):\n\n"
                        + resetLink + "\n\nIf you did not request a password reset, ignore this email.",
                "Password reset email");
    }

    public void sendTest(String toEmail, String recipientName) {
        send(toEmail, "Prueba de envío — Control Tower",
                "Hola " + recipientName + ",\n\n"
                        + "Este es un correo de prueba enviado desde Control Tower vía Resend.\n\n"
                        + "Si lo recibiste, el envío de correo está funcionando correctamente.",
                "Test email");
    }

    public void sendWelcome(String toEmail, String fullName) {
        send(toEmail, "Welcome to Control Tower!",
                "Hi " + fullName + ",\n\nYour Control Tower account has been created. "
                        + "You can now log in and start managing your operations.\n\nWelcome aboard!",
                "Welcome email");
    }

    public void sendProposal(String toEmail, String clientName, String proposalNumber,
                              String proposalTitle, String total, String validUntil,
                              String proposalUrl) {
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
        send(toEmail, "Propuesta Económica " + proposalNumber + " — " + proposalTitle, body,
                "Proposal email " + proposalNumber);
    }

    public void sendFinanceReport(String toEmail, String from, String to,
                                   String grandTotal, List<String> summaryLines) {
        StringBuilder body = new StringBuilder();
        body.append("Reporte de Gastos\n");
        body.append("Período: ").append(from).append(" — ").append(to).append("\n");
        body.append("Total: ").append(grandTotal).append("\n\n");
        body.append("Desglose por categoría:\n");
        if (summaryLines != null) {
            summaryLines.forEach(line -> body.append("  ").append(line).append("\n"));
        }
        body.append("\nGenerado automáticamente por Control Tower.");
        send(toEmail, "Reporte de Gastos — " + from + " al " + to, body.toString(), "Finance report email");
    }

    public void sendTicketCommentNotification(String toEmail, String ticketTitle,
                                               String agentName, String commentContent) {
        String body =
                "Hola,\n\n"
                + agentName + " ha respondido en tu ticket \"" + ticketTitle + "\":\n\n"
                + "---\n" + commentContent + "\n---\n\n"
                + "Si tienes alguna duda, puedes responder directamente a este correo o acceder al portal.\n\n"
                + "Atentamente,\nControl Tower";
        send(toEmail, "Nueva respuesta en tu ticket: " + ticketTitle, body,
                "Ticket comment notification (ticket '" + ticketTitle + "')");
    }

    public void sendChatReplyNotification(String toEmail, String visitorName,
                                           String agentName, String messageContent) {
        String body =
                "Hola " + visitorName + ",\n\n"
                + agentName + " te ha enviado un mensaje en el chat de soporte:\n\n"
                + "---\n" + messageContent + "\n---\n\n"
                + "Ingresa al chat para continuar la conversación.\n\n"
                + "Atentamente,\nControl Tower";
        send(toEmail, "Tienes un nuevo mensaje de soporte", body, "Chat reply notification");
    }

    public void sendKanbanCardNotification(String toEmail, String assigneeName,
                                            String cardTitle, String movedBy,
                                            String fromColumn, String toColumn) {
        String body =
                "Hola " + assigneeName + ",\n\n"
                + movedBy + " movió la tarjeta \"" + cardTitle + "\":\n\n"
                + "  De: " + fromColumn + "\n"
                + "  A:  " + toColumn + "\n\n"
                + "Ingresa a Control Tower para ver los detalles.\n\n"
                + "Atentamente,\nControl Tower";
        send(toEmail, "Actualización en tarjeta: " + cardTitle, body,
                "Kanban card notification (card '" + cardTitle + "')");
    }

    public void sendNoteNotification(String toEmail, String recipientName,
                                      String authorName, String noteTitle, String noteContent,
                                      String linkedTo, boolean isReply) {
        String action = isReply ? "respondió una nota interna" : "escribió una nota interna";
        String context = linkedTo != null ? " en " + linkedTo.toLowerCase().replace("_", " ") : "";
        String preview = noteContent != null && !noteContent.isBlank()
                ? "\n\n" + noteContent.substring(0, Math.min(noteContent.length(), 300))
                : "";
        String body =
                "Hola " + recipientName + ",\n\n"
                + authorName + " " + action + context + ":\n\n"
                + "  Asunto: " + noteTitle + preview + "\n\n"
                + "Ingresa a Control Tower para ver y responder.\n\n"
                + "Atentamente,\nControl Tower";
        send(toEmail, (isReply ? "[Respuesta] " : "[Nota] ") + noteTitle, body,
                "Note notification (isReply=" + isReply + ")");
    }

    public void sendHealthIncidentNotification(String toEmail, String recipientName,
                                                String branchName, String severity, String description) {
        String body =
                "Hola " + recipientName + ",\n\n"
                + "Se detectó un incidente de salud en \"" + branchName + "\":\n\n"
                + "  Severidad: " + severity + "\n"
                + "  Descripción: " + description + "\n\n"
                + "Ingresa a Control Tower para ver los detalles y darle seguimiento.\n\n"
                + "Atentamente,\nControl Tower";
        send(toEmail, "[" + severity + "] Incidente en " + branchName, body,
                "Health incident notification (branch '" + branchName + "')");
    }

    public void sendReciboNomina(String toEmail, String employeeName, String periodLabel,
                                  String grossPay, String imssEmployee, String isr,
                                  String infonavit, String otherDeductions, String netPay,
                                  String currency) {
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
        send(toEmail, "Recibo de Nómina — " + periodLabel, body, "Payroll receipt");
    }
}
