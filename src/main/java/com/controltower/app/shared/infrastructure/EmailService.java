package com.controltower.app.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper around JavaMailSender.
 * If mail is not configured, logs a warning and silently skips sending.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@controltower.io}")
    private String from;

    public void sendPasswordReset(String toEmail, String resetLink) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Reset your Control Tower password");
            message.setText("Click the link below to reset your password (expires in 1 hour):\n\n"
                    + resetLink + "\n\nIf you did not request a password reset, ignore this email.");
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendWelcome(String toEmail, String fullName) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Welcome email for {} ({})", toEmail, fullName);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Welcome to Control Tower!");
            message.setText("Hi " + fullName + ",\n\nYour Control Tower account has been created. "
                    + "You can now log in and start managing your operations.\n\nWelcome aboard!");
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send welcome email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendProposal(String toEmail, String clientName, String proposalNumber,
                              String proposalTitle, String total, String validUntil,
                              String proposalUrl) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Proposal {} for {}", proposalNumber, toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Propuesta Económica " + proposalNumber + " — " + proposalTitle);
            message.setText(
                    "Estimado/a " + clientName + ",\n\n"
                    + "Le enviamos la propuesta económica " + proposalNumber + ":\n\n"
                    + "Título: " + proposalTitle + "\n"
                    + "Total: " + total + "\n"
                    + "Válida hasta: " + validUntil + "\n\n"
                    + (proposalUrl != null && !proposalUrl.isBlank()
                            ? "Ver propuesta: " + proposalUrl + "\n\n" : "")
                    + "Para aceptar o rechazar esta propuesta, por favor contáctenos.\n\n"
                    + "Atentamente,\nControl Tower");
            mailSender.send(message);
            log.info("Proposal email {} sent to {}", proposalNumber, toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send proposal email {} to {}: {}", proposalNumber, toEmail, ex.getMessage());
        }
    }

    public void sendFinanceReport(String toEmail, String from, String to,
                                   String grandTotal, List<String> summaryLines) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Finance report to {}", toEmail);
            return;
        }
        try {
            StringBuilder body = new StringBuilder();
            body.append("Reporte de Gastos\n");
            body.append("Período: ").append(from).append(" — ").append(to).append("\n");
            body.append("Total: ").append(grandTotal).append("\n\n");
            body.append("Desglose por categoría:\n");
            if (summaryLines != null) {
                summaryLines.forEach(line -> body.append("  ").append(line).append("\n"));
            }
            body.append("\nGenerado automáticamente por Control Tower.");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.from);
            message.setTo(toEmail);
            message.setSubject("Reporte de Gastos — " + from + " al " + to);
            message.setText(body.toString());
            mailSender.send(message);
            log.info("Finance report email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send finance report to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendTicketCommentNotification(String toEmail, String ticketTitle,
                                               String agentName, String commentContent) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Ticket comment notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Nueva respuesta en tu ticket: " + ticketTitle);
            message.setText(
                    "Hola,\n\n"
                    + agentName + " ha respondido en tu ticket \"" + ticketTitle + "\":\n\n"
                    + "---\n" + commentContent + "\n---\n\n"
                    + "Si tienes alguna duda, puedes responder directamente a este correo o acceder al portal.\n\n"
                    + "Atentamente,\nControl Tower");
            mailSender.send(message);
            log.info("Ticket comment notification sent to {} for ticket '{}'", toEmail, ticketTitle);
        } catch (MailException ex) {
            log.warn("Failed to send ticket comment notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendChatReplyNotification(String toEmail, String visitorName,
                                           String agentName, String messageContent) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Chat reply notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Tienes un nuevo mensaje de soporte");
            message.setText(
                    "Hola " + visitorName + ",\n\n"
                    + agentName + " te ha enviado un mensaje en el chat de soporte:\n\n"
                    + "---\n" + messageContent + "\n---\n\n"
                    + "Ingresa al chat para continuar la conversación.\n\n"
                    + "Atentamente,\nControl Tower");
            mailSender.send(message);
            log.info("Chat reply notification sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send chat reply notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendKanbanCardNotification(String toEmail, String assigneeName,
                                            String cardTitle, String movedBy,
                                            String fromColumn, String toColumn) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Kanban card notification for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Actualización en tarjeta: " + cardTitle);
            message.setText(
                    "Hola " + assigneeName + ",\n\n"
                    + movedBy + " movió la tarjeta \"" + cardTitle + "\":\n\n"
                    + "  De: " + fromColumn + "\n"
                    + "  A:  " + toColumn + "\n\n"
                    + "Ingresa a Control Tower para ver los detalles.\n\n"
                    + "Atentamente,\nControl Tower");
            mailSender.send(message);
            log.info("Kanban card notification sent to {} for card '{}'", toEmail, cardTitle);
        } catch (MailException ex) {
            log.warn("Failed to send kanban card notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendReciboNomina(String toEmail, String employeeName, String periodLabel,
                                  String grossPay, String imssEmployee, String isr,
                                  String infonavit, String otherDeductions, String netPay,
                                  String currency) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Payroll receipt to {}", toEmail);
            return;
        }
        try {
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

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.from);
            message.setTo(toEmail);
            message.setSubject("Recibo de Nómina — " + periodLabel);
            message.setText(body);
            mailSender.send(message);
            log.info("Payroll receipt sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send payroll receipt to {}: {}", toEmail, ex.getMessage());
        }
    }
}
