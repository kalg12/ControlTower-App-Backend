package com.controltower.app.finance.application;

import com.controltower.app.clients.domain.Client;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.finance.api.dto.*;
import com.controltower.app.finance.domain.*;
import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final ClientRepository clientRepository;

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

        Payment saved = paymentRepository.save(payment);

        // Auto-mark linked invoice as PAID
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

        // Build monthly breakdown
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

    private Map<UUID, Client> loadClients(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return clientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Client::getId, c -> c));
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
                items, i.getCreatedAt(), i.getUpdatedAt());
    }

    private PaymentResponse toPaymentResponse(Payment p, Map<UUID, Client> clients) {
        Client client = p.getClientId() != null ? clients.get(p.getClientId()) : null;
        return new PaymentResponse(
                p.getId(), p.getTenantId(), p.getClientId(),
                client != null ? client.getName() : null,
                p.getInvoiceId(), p.getAmount(), p.getCurrency(), p.getMethod(),
                p.getReference(), p.getNotes(), p.getPaidAt(), p.getCreatedAt());
    }

    private ExpenseResponse toExpenseResponse(Expense e, Map<UUID, Client> clients) {
        Client client = e.getClientId() != null ? clients.get(e.getClientId()) : null;
        return new ExpenseResponse(
                e.getId(), e.getTenantId(), e.getClientId(),
                client != null ? client.getName() : null,
                e.getCategory(), e.getDescription(),
                e.getAmount(), e.getCurrency(), e.getVendor(), e.getReceiptUrl(),
                e.getNotes(), e.getPaidAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
