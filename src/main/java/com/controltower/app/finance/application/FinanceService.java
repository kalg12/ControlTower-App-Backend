package com.controltower.app.finance.application;

import com.controltower.app.audit.application.AuditService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.clients.domain.Client;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.finance.api.dto.*;
import com.controltower.app.finance.domain.*;
import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    // ── INVOICES ─────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setClientId(req.clientId());
        invoice.setNumber(generateInvoiceNumber(tenantId));
        invoice.setCurrency(req.currency() != null ? req.currency() : "MXN");
        if (req.taxRate() != null) invoice.setTaxRate(req.taxRate());
        invoice.setNotes(req.notes());
        invoice.setIssuedAt(req.issuedAt() != null ? req.issuedAt() : LocalDate.now());
        invoice.setDueDate(req.dueDate());
        applyRecurring(invoice, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        setLineItems(invoice, req.lineItems());
        invoice.recalculate();

        Invoice saved = invoiceRepository.save(invoice);
        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toInvoiceResponse(saved, clients);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID id, InvoiceRequest req) {
        Invoice invoice = requireInvoice(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be updated");
        }

        invoice.setClientId(req.clientId());
        if (req.currency() != null) invoice.setCurrency(req.currency());
        if (req.taxRate() != null) invoice.setTaxRate(req.taxRate());
        invoice.setNotes(req.notes());
        if (req.issuedAt() != null) invoice.setIssuedAt(req.issuedAt());
        invoice.setDueDate(req.dueDate());
        applyRecurring(invoice, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        setLineItems(invoice, req.lineItems());
        invoice.recalculate();

        Invoice saved = invoiceRepository.save(invoice);
        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toInvoiceResponse(saved, clients);
    }

    @Transactional
    public InvoiceResponse markSent(UUID id) {
        Invoice invoice = requireInvoice(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice is not in DRAFT status");
        }
        invoice.setStatus(InvoiceStatus.SENT);
        Invoice saved = invoiceRepository.save(invoice);
        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toInvoiceResponse(saved, clients);
    }

    @Transactional
    public InvoiceResponse markPaid(UUID id, MarkPaidRequest req) {
        Invoice invoice = requireInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already PAID");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new IllegalStateException("Cannot mark a " + invoice.getStatus() + " invoice as PAID");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(req != null && req.paidAt() != null ? req.paidAt() : Instant.now());
        Invoice saved = invoiceRepository.save(invoice);
        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toInvoiceResponse(saved, clients);
    }

    @Transactional
    public InvoiceResponse voidInvoice(UUID id) {
        Invoice invoice = requireInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot void a PAID invoice");
        }
        invoice.setStatus(InvoiceStatus.VOIDED);
        Invoice saved = invoiceRepository.save(invoice);
        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toInvoiceResponse(saved, clients);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(InvoiceStatus status, UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Invoice> page = invoiceRepository.findFiltered(tenantId, status, clientId, pageable);
        Map<UUID, Client> clients = loadClients(page.stream()
                .map(Invoice::getClientId).filter(Objects::nonNull).collect(Collectors.toList()));
        return page.map(i -> toInvoiceResponse(i, clients));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        Invoice i = requireInvoice(id);
        Map<UUID, Client> clients = i.getClientId() != null
                ? loadClients(List.of(i.getClientId())) : Map.of();
        return toInvoiceResponse(i, clients);
    }

    @Transactional
    public void deleteInvoice(UUID id) {
        Invoice invoice = requireInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be deleted");
        }
        invoice.softDelete();
        invoiceRepository.save(invoice);
    }

    // ── PAYMENTS ─────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse createPayment(PaymentRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setClientId(req.clientId());
        payment.setInvoiceId(req.invoiceId());
        payment.setAmount(req.amount());
        payment.setCurrency(req.currency() != null ? req.currency() : "MXN");
        payment.setMethod(req.method() != null ? req.method() : Payment.PaymentMethod.BANK_TRANSFER);
        payment.setReference(req.reference());
        payment.setNotes(req.notes());
        payment.setPaidAt(req.paidAt() != null ? req.paidAt() : Instant.now());
        payment.setSource(req.source() != null ? req.source() : Payment.PaymentSource.MANUAL);
        payment.setPosReference(req.posReference());
        applyRecurring(payment, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        Payment saved = paymentRepository.save(payment);

        if (req.invoiceId() != null) {
            invoiceRepository.findByIdAndDeletedAtIsNull(req.invoiceId()).ifPresent(inv -> {
                if (inv.getStatus() != InvoiceStatus.PAID && inv.getStatus() != InvoiceStatus.CANCELLED && inv.getStatus() != InvoiceStatus.VOIDED) {
                    inv.setStatus(InvoiceStatus.PAID);
                    inv.setPaidAt(saved.getPaidAt());
                    invoiceRepository.save(inv);
                }
            });
        }

        Map<UUID, Client> clients = saved.getClientId() != null
                ? loadClients(List.of(saved.getClientId())) : Map.of();
        return toPaymentResponse(saved, clients);
    }

    @Transactional
    public void deletePayment(UUID id) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        payment.softDelete();
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Payment> page = paymentRepository.findFiltered(tenantId, clientId, pageable);
        Map<UUID, Client> clients = loadClients(page.stream()
                .map(Payment::getClientId).filter(Objects::nonNull).collect(Collectors.toList()));
        return page.map(p -> toPaymentResponse(p, clients));
    }

    // ── EXPENSES ─────────────────────────────────────────────────────────────

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        Expense expense = new Expense();
        expense.setTenantId(tenantId);
        expense.setClientId(req.clientId());
        expense.setCategory(req.category() != null ? req.category() : Expense.ExpenseCategory.OTHER);
        expense.setDescription(req.description());
        expense.setAmount(req.amount());
        expense.setCurrency(req.currency() != null ? req.currency() : "MXN");
        expense.setVendor(req.vendor());
        expense.setReceiptUrl(req.receiptUrl());
        expense.setNotes(req.notes());
        expense.setPaidAt(req.paidAt() != null ? req.paidAt() : Instant.now());
        applyRecurring(expense, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        return toExpenseResponse(expenseRepository.save(expense), Map.of());
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, ExpenseRequest req) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));

        expense.setClientId(req.clientId());
        if (req.category() != null) expense.setCategory(req.category());
        expense.setDescription(req.description());
        expense.setAmount(req.amount());
        if (req.currency() != null) expense.setCurrency(req.currency());
        expense.setVendor(req.vendor());
        expense.setReceiptUrl(req.receiptUrl());
        expense.setNotes(req.notes());
        if (req.paidAt() != null) expense.setPaidAt(req.paidAt());
        applyRecurring(expense, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        return toExpenseResponse(expenseRepository.save(expense), Map.of());
    }

    @Transactional
    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
        expense.softDelete();
        expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> listExpenses(Expense.ExpenseCategory category, UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Expense> page = expenseRepository.findFiltered(tenantId, category, clientId, pageable);
        Map<UUID, Client> clients = loadClients(page.stream()
                .map(Expense::getClientId).filter(Objects::nonNull).collect(Collectors.toList()));
        return page.map(e -> toExpenseResponse(e, clients));
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> listExpensesAdvanced(
            Expense.ExpenseCategory category, UUID clientId, String vendor,
            BigDecimal amountMin, BigDecimal amountMax,
            Instant from, Instant to, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Expense> page = expenseRepository.findFilteredAdvanced(
                tenantId, category, clientId, vendor, amountMin, amountMax, from, to, pageable);
        Map<UUID, Client> clients = loadClients(page.stream()
                .map(Expense::getClientId).filter(Objects::nonNull).collect(Collectors.toList()));
        return page.map(e -> toExpenseResponse(e, clients));
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(Instant from, Instant to) {
        if (to.toEpochMilli() - from.toEpochMilli() > 366L * 24 * 60 * 60 * 1000) {
            throw new IllegalArgumentException("Date range must not exceed 366 days");
        }
        UUID tenantId = TenantContext.getTenantId();
        List<Expense> expenses = expenseRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);

        BigDecimal grandTotal = expenses.stream()
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Expense>> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().name()));

        List<ExpenseSummaryResponse.CategoryBreakdown> categoryBreakdowns = byCategory.entrySet().stream()
                .map(entry -> {
                    BigDecimal catTotal = entry.getValue().stream()
                            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : catTotal.multiply(new BigDecimal("100"))
                              .divide(grandTotal, 2, RoundingMode.HALF_UP).doubleValue();
                    return new ExpenseSummaryResponse.CategoryBreakdown(
                            entry.getKey(), catTotal, entry.getValue().size(), pct);
                })
                .sorted(Comparator.comparing(ExpenseSummaryResponse.CategoryBreakdown::total).reversed())
                .collect(Collectors.toList());

        Map<YearMonth, Map<String, BigDecimal>> monthCatMap = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthTotalMap = new TreeMap<>();
        for (Expense e : expenses) {
            YearMonth ym = YearMonth.from(e.getPaidAt().atZone(ZoneOffset.UTC));
            String cat = e.getCategory().name();
            monthCatMap.computeIfAbsent(ym, k -> new LinkedHashMap<>())
                    .merge(cat, e.getAmount(), BigDecimal::add);
            monthTotalMap.merge(ym, e.getAmount(), BigDecimal::add);
        }

        List<ExpenseSummaryResponse.MonthlyBreakdown> byMonth = monthTotalMap.entrySet().stream()
                .map(entry -> new ExpenseSummaryResponse.MonthlyBreakdown(
                        entry.getKey().toString(),
                        entry.getValue(),
                        monthCatMap.getOrDefault(entry.getKey(), Map.of())))
                .collect(Collectors.toList());

        return new ExpenseSummaryResponse(from, to, grandTotal, categoryBreakdowns, byMonth);
    }

    @Transactional
    public void sendFinanceReportEmail(String toEmail, Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        ExpenseSummaryResponse summary = getExpenseSummary(from, to);

        String grandTotal = String.format("$%,.2f MXN", summary.grandTotal());
        List<String> lines = summary.byCategory().stream()
                .map(cb -> String.format("%s: $%,.2f (%.1f%%)", cb.category(), cb.total(), cb.percentage()))
                .collect(Collectors.toList());

        String fromLabel = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC).format(from);
        String toLabel = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC).format(to);

        emailService.sendFinanceReport(toEmail, fromLabel, toLabel, grandTotal, lines);
        auditService.log(AuditAction.FINANCE_REPORT_SENT, tenantId, userId, "FinanceReport", toEmail);
    }

    // ── PURCHASES ────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setTenantId(tenantId);
        purchase.setVendor(req.vendor());
        purchase.setDescription(req.description());
        purchase.setAmount(req.amount());
        purchase.setCurrency(req.currency() != null ? req.currency() : "MXN");
        purchase.setCategory(req.category() != null ? req.category() : Expense.ExpenseCategory.OTHER);
        purchase.setQuantity(req.quantity() != null ? req.quantity() : BigDecimal.ONE);
        purchase.setUnitPrice(req.unitPrice());
        purchase.setReceiptUrl(req.receiptUrl());
        purchase.setNotes(req.notes());
        purchase.setPurchasedAt(req.purchasedAt() != null ? req.purchasedAt() : Instant.now());
        purchase.setSource(req.source() != null ? req.source() : PurchaseRecord.PurchaseSource.MANUAL);
        purchase.setPosReference(req.posReference());
        applyRecurringPurchase(purchase, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        return toPurchaseResponse(purchaseRecordRepository.save(purchase));
    }

    @Transactional
    public PurchaseResponse updatePurchase(UUID id, PurchaseRequest req) {
        PurchaseRecord purchase = purchaseRecordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));

        purchase.setVendor(req.vendor());
        purchase.setDescription(req.description());
        purchase.setAmount(req.amount());
        if (req.currency() != null) purchase.setCurrency(req.currency());
        if (req.category() != null) purchase.setCategory(req.category());
        if (req.quantity() != null) purchase.setQuantity(req.quantity());
        purchase.setUnitPrice(req.unitPrice());
        purchase.setReceiptUrl(req.receiptUrl());
        purchase.setNotes(req.notes());
        if (req.purchasedAt() != null) purchase.setPurchasedAt(req.purchasedAt());
        if (req.source() != null) purchase.setSource(req.source());
        purchase.setPosReference(req.posReference());
        applyRecurringPurchase(purchase, req.isRecurring(), req.recurrenceType(), req.recurrenceEndDate());

        return toPurchaseResponse(purchaseRecordRepository.save(purchase));
    }

    @Transactional
    public void deletePurchase(UUID id) {
        PurchaseRecord purchase = purchaseRecordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        purchase.softDelete();
        purchaseRecordRepository.save(purchase);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> listPurchases(
            PurchaseRecord.PurchaseSource source,
            Expense.ExpenseCategory category,
            String vendor,
            Instant from,
            Instant to,
            Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return purchaseRecordRepository
                .findFiltered(tenantId, source, category, vendor, from, to, pageable)
                .map(this::toPurchaseResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(UUID id) {
        PurchaseRecord p = purchaseRecordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        return toPurchaseResponse(p);
    }

    // ── P&L REPORT ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PnlReportResponse getPnlReport(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();

        List<Payment> payments = paymentRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);
        List<Expense> expenses = expenseRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);
        List<PurchaseRecord> purchases = purchaseRecordRepository.findByTenantIdAndPurchasedAtBetween(tenantId, from, to);

        BigDecimal totalIncome = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = purchases.stream().map(PurchaseRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutflows = totalExpenses.add(totalPurchases);
        BigDecimal netProfit = totalIncome.subtract(totalOutflows);

        double marginPct = totalIncome.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : netProfit.multiply(new BigDecimal("100"))
                  .divide(totalIncome, 2, RoundingMode.HALF_UP).doubleValue();

        // Monthly breakdown
        Map<YearMonth, BigDecimal[]> monthly = new TreeMap<>();
        for (Payment p : payments) {
            YearMonth ym = YearMonth.from(p.getPaidAt().atZone(ZoneOffset.UTC));
            monthly.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO})[0] =
                    monthly.get(ym)[0].add(p.getAmount());
        }
        for (Expense e : expenses) {
            YearMonth ym = YearMonth.from(e.getPaidAt().atZone(ZoneOffset.UTC));
            monthly.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO})[1] =
                    monthly.get(ym)[1].add(e.getAmount());
        }
        for (PurchaseRecord pr : purchases) {
            YearMonth ym = YearMonth.from(pr.getPurchasedAt().atZone(ZoneOffset.UTC));
            monthly.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO})[2] =
                    monthly.get(ym)[2].add(pr.getAmount());
        }

        List<PnlReportResponse.MonthlyPnl> byMonth = monthly.entrySet().stream()
                .map(e -> new PnlReportResponse.MonthlyPnl(
                        e.getKey().toString(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2],
                        e.getValue()[0].subtract(e.getValue()[1]).subtract(e.getValue()[2])
                ))
                .collect(Collectors.toList());

        // Category breakdowns
        Map<String, BigDecimal> incomeByCategory = Map.of("PAYMENTS", totalIncome);
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        return new PnlReportResponse(from, to, totalIncome, totalOutflows, netProfit, marginPct,
                byMonth, incomeByCategory, expensesByCategory);
    }

    // ── CLIENT SUMMARY ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClientFinanceSummaryResponse getClientSummary(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        BigDecimal totalInvoiced = invoiceRepository.sumTotalByClientId(clientId);
        BigDecimal totalPaid = invoiceRepository.sumPaidByClientId(clientId);
        BigDecimal totalOutstanding = totalInvoiced.subtract(totalPaid);
        BigDecimal totalExpenses = expenseRepository.sumAmountByClientId(clientId);
        long invoiceCount = invoiceRepository.countByClientIdAndDeletedAtIsNull(clientId);
        long paymentCount = paymentRepository.countByClientIdAndDeletedAtIsNull(clientId);
        long expenseCount = expenseRepository.countByClientIdAndDeletedAtIsNull(clientId);
        Instant lastInvoiceAt = invoiceRepository.findLastInvoiceAtByClientId(clientId);

        return new ClientFinanceSummaryResponse(
                clientId, client.getName(),
                totalInvoiced, totalPaid, totalOutstanding, totalExpenses,
                invoiceCount, paymentCount, expenseCount, lastInvoiceAt);
    }

    // ── CASH FLOW ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CashFlowResponse getCashFlow(Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId();

        List<Payment> payments = paymentRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);
        List<Expense> expenses = expenseRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);

        BigDecimal totalIncome = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netFlow = totalIncome.subtract(totalExpenses);

        Map<YearMonth, BigDecimal[]> monthly = new TreeMap<>();
        for (Payment p : payments) {
            YearMonth ym = YearMonth.from(p.getPaidAt().atZone(ZoneOffset.UTC));
            monthly.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] =
                    monthly.get(ym)[0].add(p.getAmount());
        }
        for (Expense e : expenses) {
            YearMonth ym = YearMonth.from(e.getPaidAt().atZone(ZoneOffset.UTC));
            monthly.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] =
                    monthly.get(ym)[1].add(e.getAmount());
        }

        List<CashFlowResponse.MonthlyEntry> byMonth = monthly.entrySet().stream()
                .map(e -> new CashFlowResponse.MonthlyEntry(
                        e.getKey().toString(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[0].subtract(e.getValue()[1])
                ))
                .collect(Collectors.toList());

        return new CashFlowResponse(totalIncome, totalExpenses, netFlow, byMonth);
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────

    private Invoice requireInvoice(UUID id) {
        return invoiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private String generateInvoiceNumber(UUID tenantId) {
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.countByTenantIdAndYear(tenantId, year);
        return String.format("INV-%d-%04d", year, count + 1);
    }

    private void setLineItems(Invoice invoice, List<InvoiceLineItemRequest> reqs) {
        invoice.getLineItems().clear();
        int pos = 0;
        for (InvoiceLineItemRequest r : reqs) {
            InvoiceLineItem item = new InvoiceLineItem();
            item.setInvoice(invoice);
            item.setDescription(r.description());
            item.setQuantity(r.quantity());
            item.setUnitPrice(r.unitPrice());
            item.setPosition(r.position() > 0 ? r.position() : pos);
            item.computeTotal();
            invoice.getLineItems().add(item);
            pos++;
        }
    }

    private void applyRecurring(Invoice invoice, Boolean isRecurring, RecurrenceType type, LocalDate endDate) {
        if (Boolean.TRUE.equals(isRecurring)) {
            invoice.setRecurring(true);
            invoice.setRecurrenceType(type);
            invoice.setRecurrenceEndDate(endDate);
            if (invoice.getNextOccurrenceDate() == null && type != null) {
                invoice.setNextOccurrenceDate(nextOccurrence(invoice.getIssuedAt() != null ? invoice.getIssuedAt() : LocalDate.now(), type));
            }
        } else if (Boolean.FALSE.equals(isRecurring)) {
            invoice.setRecurring(false);
            invoice.setRecurrenceType(null);
            invoice.setRecurrenceEndDate(null);
            invoice.setNextOccurrenceDate(null);
        }
    }

    private void applyRecurring(Payment payment, Boolean isRecurring, RecurrenceType type, LocalDate endDate) {
        if (Boolean.TRUE.equals(isRecurring)) {
            payment.setRecurring(true);
            payment.setRecurrenceType(type);
            payment.setRecurrenceEndDate(endDate);
            if (payment.getNextOccurrenceDate() == null && type != null) {
                payment.setNextOccurrenceDate(nextOccurrence(LocalDate.now(), type));
            }
        } else if (Boolean.FALSE.equals(isRecurring)) {
            payment.setRecurring(false);
            payment.setRecurrenceType(null);
            payment.setRecurrenceEndDate(null);
            payment.setNextOccurrenceDate(null);
        }
    }

    private void applyRecurring(Expense expense, Boolean isRecurring, RecurrenceType type, LocalDate endDate) {
        if (Boolean.TRUE.equals(isRecurring)) {
            expense.setRecurring(true);
            expense.setRecurrenceType(type);
            expense.setRecurrenceEndDate(endDate);
            if (expense.getNextOccurrenceDate() == null && type != null) {
                expense.setNextOccurrenceDate(nextOccurrence(LocalDate.now(), type));
            }
        } else if (Boolean.FALSE.equals(isRecurring)) {
            expense.setRecurring(false);
            expense.setRecurrenceType(null);
            expense.setRecurrenceEndDate(null);
            expense.setNextOccurrenceDate(null);
        }
    }

    private void applyRecurringPurchase(PurchaseRecord purchase, Boolean isRecurring, RecurrenceType type, LocalDate endDate) {
        if (Boolean.TRUE.equals(isRecurring)) {
            purchase.setRecurring(true);
            purchase.setRecurrenceType(type);
            purchase.setRecurrenceEndDate(endDate);
            if (purchase.getNextOccurrenceDate() == null && type != null) {
                purchase.setNextOccurrenceDate(nextOccurrence(LocalDate.now(), type));
            }
        } else if (Boolean.FALSE.equals(isRecurring)) {
            purchase.setRecurring(false);
            purchase.setRecurrenceType(null);
            purchase.setRecurrenceEndDate(null);
            purchase.setNextOccurrenceDate(null);
        }
    }

    private LocalDate nextOccurrence(LocalDate base, RecurrenceType type) {
        return switch (type) {
            case DAILY     -> base.plusDays(1);
            case WEEKLY    -> base.plusWeeks(1);
            case BIWEEKLY  -> base.plusWeeks(2);
            case MONTHLY   -> base.plusMonths(1);
            case QUARTERLY -> base.plusMonths(3);
            case YEARLY    -> base.plusYears(1);
        };
    }

    private Map<UUID, Client> loadClients(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return clientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Client::getId, c -> c));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private InvoiceResponse toInvoiceResponse(Invoice i, Map<UUID, Client> clients) {
        Client client = i.getClientId() != null ? clients.get(i.getClientId()) : null;
        List<InvoiceLineItemResponse> items = i.getLineItems().stream()
                .map(li -> new InvoiceLineItemResponse(
                        li.getId(), li.getDescription(), li.getQuantity(),
                        li.getUnitPrice(), li.getTotal(), li.getPosition(), li.getCreatedAt()))
                .collect(Collectors.toList());

        return new InvoiceResponse(
                i.getId(), i.getTenantId(), i.getClientId(),
                client != null ? client.getName() : null,
                client != null ? client.getTaxId() : null,
                i.getNumber(), i.getStatus(), i.getSubtotal(), i.getTaxRate(), i.getTaxAmount(),
                i.getTotal(), i.getCurrency(), i.getNotes(),
                i.getIssuedAt(), i.getDueDate(), i.getPaidAt(),
                items, i.getCreatedAt(), i.getUpdatedAt(),
                i.isRecurring(), i.getRecurrenceType(), i.getRecurrenceEndDate(),
                i.getNextOccurrenceDate(), i.getParentRecurringId());
    }

    private PaymentResponse toPaymentResponse(Payment p, Map<UUID, Client> clients) {
        Client client = p.getClientId() != null ? clients.get(p.getClientId()) : null;
        return new PaymentResponse(
                p.getId(), p.getTenantId(), p.getClientId(),
                client != null ? client.getName() : null,
                p.getInvoiceId(), p.getAmount(), p.getCurrency(), p.getMethod(),
                p.getReference(), p.getNotes(), p.getPaidAt(), p.getCreatedAt(),
                p.getSource(), p.getPosReference(),
                p.isRecurring(), p.getRecurrenceType(), p.getRecurrenceEndDate(),
                p.getNextOccurrenceDate(), p.getParentRecurringId());
    }

    private ExpenseResponse toExpenseResponse(Expense e, Map<UUID, Client> clients) {
        Client client = e.getClientId() != null ? clients.get(e.getClientId()) : null;
        return new ExpenseResponse(
                e.getId(), e.getTenantId(), e.getClientId(),
                client != null ? client.getName() : null,
                e.getCategory(), e.getDescription(),
                e.getAmount(), e.getCurrency(), e.getVendor(), e.getReceiptUrl(),
                e.getNotes(), e.getPaidAt(), e.getCreatedAt(), e.getUpdatedAt(),
                e.isRecurring(), e.getRecurrenceType(), e.getRecurrenceEndDate(),
                e.getNextOccurrenceDate(), e.getParentRecurringId());
    }

    private PurchaseResponse toPurchaseResponse(PurchaseRecord p) {
        return new PurchaseResponse(
                p.getId(), p.getTenantId(), p.getVendor(), p.getDescription(),
                p.getAmount(), p.getCurrency(), p.getCategory(), p.getQuantity(), p.getUnitPrice(),
                p.getReceiptUrl(), p.getNotes(), p.getPurchasedAt(), p.getSource(), p.getPosReference(),
                p.isRecurring(), p.getRecurrenceType(), p.getRecurrenceEndDate(),
                p.getNextOccurrenceDate(), p.getParentRecurringId(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
