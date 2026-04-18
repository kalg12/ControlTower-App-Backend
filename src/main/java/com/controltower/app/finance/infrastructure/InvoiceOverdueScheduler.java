package com.controltower.app.finance.infrastructure;

import com.controltower.app.finance.domain.Invoice;
import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.finance.domain.InvoiceRepository;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceOverdueScheduler {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkOverdueAndDueSoon() {
        LocalDate today = LocalDate.now();

        markAndNotifyOverdue(today);
        notifyDueSoon(today.plusDays(1), 1);
        notifyDueSoon(today.plusDays(5), 5);
    }

    private void markAndNotifyOverdue(LocalDate today) {
        List<Invoice> overdue = invoiceRepository.findOverdueInvoices(today);
        for (Invoice invoice : overdue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);

            List<UUID> recipients = getFinanceUsers(invoice.getTenantId());
            if (recipients.isEmpty()) continue;

            notificationService.send(
                    invoice.getTenantId(),
                    "INVOICE_OVERDUE",
                    "Factura vencida",
                    "La factura " + invoice.getNumber() + " venció el " + invoice.getDueDate(),
                    Notification.Severity.ERROR,
                    Map.of("invoiceId", invoice.getId().toString(), "invoiceNumber", invoice.getNumber()),
                    recipients);
        }
        if (!overdue.isEmpty()) log.info("Marked {} invoices as OVERDUE", overdue.size());
    }

    private void notifyDueSoon(LocalDate dueDate, int daysAway) {
        List<Invoice> dueSoon = invoiceRepository.findInvoicesDueOn(dueDate);
        for (Invoice invoice : dueSoon) {
            List<UUID> recipients = getFinanceUsers(invoice.getTenantId());
            if (recipients.isEmpty()) continue;

            String dayLabel = daysAway == 1 ? "mañana" : "en " + daysAway + " días";
            notificationService.send(
                    invoice.getTenantId(),
                    "INVOICE_DUE_SOON",
                    "Factura por vencer",
                    "La factura " + invoice.getNumber() + " vence " + dayLabel + " (" + dueDate + ")",
                    daysAway == 1 ? Notification.Severity.WARNING : Notification.Severity.INFO,
                    Map.of("invoiceId", invoice.getId().toString(), "invoiceNumber", invoice.getNumber(), "daysAway", daysAway),
                    recipients);
        }
    }

    private List<UUID> getFinanceUsers(UUID tenantId) {
        return userRepository.findByTenantIdAndPermission(tenantId, "finance:read")
                .stream().map(u -> u.getId()).collect(Collectors.toList());
    }
}
