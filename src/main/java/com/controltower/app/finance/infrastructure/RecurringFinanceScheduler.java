package com.controltower.app.finance.infrastructure;

import com.controltower.app.finance.domain.*;
import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringFinanceScheduler {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void generateRecurringTransactions() {
        LocalDate today = LocalDate.now();
        log.info("Running recurring finance generation for {}", today);

        generateInvoices(today);
        generatePayments(today);
        generateExpenses(today);
        generatePurchases(today);
    }

    private void generateInvoices(LocalDate today) {
        List<Invoice> due = invoiceRepository.findDueForRecurrence(today);
        for (Invoice parent : due) {
            Invoice clone = new Invoice();
            clone.setTenantId(parent.getTenantId());
            clone.setClientId(parent.getClientId());
            clone.setNumber(generateInvoiceNumber(parent.getTenantId()));
            clone.setStatus(InvoiceStatus.DRAFT);
            clone.setCurrency(parent.getCurrency());
            clone.setTaxRate(parent.getTaxRate());
            clone.setNotes(parent.getNotes());
            clone.setIssuedAt(today);
            clone.setDueDate(parent.getDueDate() != null
                    ? today.plusDays(parent.getIssuedAt() != null
                        ? java.time.temporal.ChronoUnit.DAYS.between(parent.getIssuedAt(), parent.getDueDate()) : 30)
                    : today.plusDays(30));
            clone.setRecurring(false);
            clone.setParentRecurringId(parent.getId());

            // Copy line items
            for (InvoiceLineItem li : parent.getLineItems()) {
                InvoiceLineItem newItem = new InvoiceLineItem();
                newItem.setInvoice(clone);
                newItem.setDescription(li.getDescription());
                newItem.setQuantity(li.getQuantity());
                newItem.setUnitPrice(li.getUnitPrice());
                newItem.setPosition(li.getPosition());
                newItem.computeTotal();
                clone.getLineItems().add(newItem);
            }
            clone.recalculate();
            invoiceRepository.save(clone);

            parent.setNextOccurrenceDate(nextDate(parent.getNextOccurrenceDate(), parent.getRecurrenceType()));
            invoiceRepository.save(parent);

            notifyFinanceUsers(parent.getTenantId(), "FINANCE_RECURRING_GENERATED",
                    "Factura recurrente generada",
                    "Se generó la factura recurrente " + clone.getNumber() + " (" + parent.getRecurrenceType() + ")",
                    Map.of("invoiceId", clone.getId().toString()));
        }
        if (!due.isEmpty()) log.info("Generated {} recurring invoices", due.size());
    }

    private void generatePayments(LocalDate today) {
        List<Payment> due = paymentRepository.findDueForRecurrence(today);
        for (Payment parent : due) {
            Payment clone = new Payment();
            clone.setTenantId(parent.getTenantId());
            clone.setClientId(parent.getClientId());
            clone.setInvoiceId(parent.getInvoiceId());
            clone.setAmount(parent.getAmount());
            clone.setCurrency(parent.getCurrency());
            clone.setMethod(parent.getMethod());
            clone.setReference(parent.getReference());
            clone.setNotes(parent.getNotes());
            clone.setPaidAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
            clone.setSource(parent.getSource());
            clone.setPosReference(parent.getPosReference());
            clone.setRecurring(false);
            clone.setParentRecurringId(parent.getId());
            paymentRepository.save(clone);

            parent.setNextOccurrenceDate(nextDate(parent.getNextOccurrenceDate(), parent.getRecurrenceType()));
            paymentRepository.save(parent);
        }
        if (!due.isEmpty()) log.info("Generated {} recurring payments", due.size());
    }

    private void generateExpenses(LocalDate today) {
        List<Expense> due = expenseRepository.findDueForRecurrence(today);
        for (Expense parent : due) {
            Expense clone = new Expense();
            clone.setTenantId(parent.getTenantId());
            clone.setClientId(parent.getClientId());
            clone.setCategory(parent.getCategory());
            clone.setDescription(parent.getDescription());
            clone.setAmount(parent.getAmount());
            clone.setCurrency(parent.getCurrency());
            clone.setVendor(parent.getVendor());
            clone.setReceiptUrl(parent.getReceiptUrl());
            clone.setNotes(parent.getNotes());
            clone.setPaidAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
            clone.setRecurring(false);
            clone.setParentRecurringId(parent.getId());
            expenseRepository.save(clone);

            parent.setNextOccurrenceDate(nextDate(parent.getNextOccurrenceDate(), parent.getRecurrenceType()));
            expenseRepository.save(parent);
        }
        if (!due.isEmpty()) log.info("Generated {} recurring expenses", due.size());
    }

    private void generatePurchases(LocalDate today) {
        List<PurchaseRecord> due = purchaseRecordRepository.findDueForRecurrence(today);
        for (PurchaseRecord parent : due) {
            PurchaseRecord clone = new PurchaseRecord();
            clone.setTenantId(parent.getTenantId());
            clone.setVendor(parent.getVendor());
            clone.setDescription(parent.getDescription());
            clone.setAmount(parent.getAmount());
            clone.setCurrency(parent.getCurrency());
            clone.setCategory(parent.getCategory());
            clone.setQuantity(parent.getQuantity());
            clone.setUnitPrice(parent.getUnitPrice());
            clone.setReceiptUrl(parent.getReceiptUrl());
            clone.setNotes(parent.getNotes());
            clone.setPurchasedAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
            clone.setSource(parent.getSource());
            clone.setPosReference(parent.getPosReference());
            clone.setRecurring(false);
            clone.setParentRecurringId(parent.getId());
            purchaseRecordRepository.save(clone);

            parent.setNextOccurrenceDate(nextDate(parent.getNextOccurrenceDate(), parent.getRecurrenceType()));
            purchaseRecordRepository.save(parent);
        }
        if (!due.isEmpty()) log.info("Generated {} recurring purchases", due.size());
    }

    private LocalDate nextDate(LocalDate from, RecurrenceType type) {
        if (from == null || type == null) return null;
        return switch (type) {
            case DAILY     -> from.plusDays(1);
            case WEEKLY    -> from.plusWeeks(1);
            case BIWEEKLY  -> from.plusWeeks(2);
            case MONTHLY   -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case YEARLY    -> from.plusYears(1);
        };
    }

    private String generateInvoiceNumber(UUID tenantId) {
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.countByTenantIdAndYear(tenantId, year);
        return String.format("INV-%d-%04d", year, count + 1);
    }

    private void notifyFinanceUsers(UUID tenantId, String type, String title, String body, Map<String, Object> meta) {
        List<UUID> recipients = userRepository.findByTenantIdAndPermission(tenantId, "finance:read")
                .stream().map(u -> u.getId()).collect(Collectors.toList());
        if (recipients.isEmpty()) return;
        notificationService.send(tenantId, type, title, body, Notification.Severity.INFO, meta, recipients);
    }
}
