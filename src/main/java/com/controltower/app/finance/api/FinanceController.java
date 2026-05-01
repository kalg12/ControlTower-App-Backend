package com.controltower.app.finance.api;

import com.controltower.app.finance.api.dto.*;
import com.controltower.app.finance.application.FinanceService;
import com.controltower.app.finance.domain.Expense.ExpenseCategory;
import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.finance.domain.PurchaseRecord.PurchaseSource;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Finance", description = "Invoices, payments and expenses")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    // ── INVOICES ─────────────────────────────────────────────────────────────

    @Operation(summary = "List invoices")
    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> listInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(financeService.listInvoices(status, clientId, pageable))));
    }

    @Operation(summary = "Get invoice")
    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getInvoice(id)));
    }

    @Operation(summary = "Create invoice")
    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(financeService.createInvoice(request)));
    }

    @Operation(summary = "Update invoice (DRAFT only)")
    @PutMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.updateInvoice(id, request)));
    }

    @Operation(summary = "Send invoice (DRAFT → SENT)")
    @PostMapping("/invoices/{id}/send")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> sendInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.markSent(id)));
    }

    @Operation(summary = "Mark invoice as paid")
    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> payInvoice(
            @PathVariable UUID id,
            @RequestBody(required = false) MarkPaidRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.markPaid(id, request)));
    }

    @Operation(summary = "Void invoice")
    @PostMapping("/invoices/{id}/void")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> voidInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.voidInvoice(id)));
    }

    @Operation(summary = "Delete invoice (DRAFT only)")
    @DeleteMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        financeService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // ── PAYMENTS ─────────────────────────────────────────────────────────────

    @Operation(summary = "List payments")
    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> listPayments(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("paidAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(financeService.listPayments(clientId, pageable))));
    }

    @Operation(summary = "Create payment")
    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(financeService.createPayment(request)));
    }

    @Operation(summary = "Delete payment")
    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id) {
        financeService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    // ── EXPENSES ─────────────────────────────────────────────────────────────

    @Operation(summary = "List expenses with advanced filters")
    @GetMapping("/expenses")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> listExpenses(
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) java.math.BigDecimal amountMin,
            @RequestParam(required = false) java.math.BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("paidAt").descending());
        if (vendor != null || amountMin != null || amountMax != null || from != null || to != null) {
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                    financeService.listExpensesAdvanced(category, clientId, vendor, amountMin, amountMax, from, to, pageable))));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(financeService.listExpenses(category, clientId, pageable))));
    }

    @Operation(summary = "Get expense summary by category and month")
    @GetMapping("/expenses/summary")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<com.controltower.app.finance.api.dto.ExpenseSummaryResponse>> getExpenseSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getExpenseSummary(from, to)));
    }

    @Operation(summary = "Send finance expense report via email")
    @PostMapping("/reports/email")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<String>> sendFinanceReport(
            @jakarta.validation.Valid @RequestBody com.controltower.app.finance.api.dto.FinanceReportEmailRequest request) {
        financeService.sendFinanceReportEmail(request.toEmail(), request.from(), request.to());
        return ResponseEntity.ok(ApiResponse.ok("Report sent successfully"));
    }

    @Operation(summary = "Create expense")
    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(financeService.createExpense(request)));
    }

    @Operation(summary = "Update expense")
    @PutMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.updateExpense(id, request)));
    }

    @Operation(summary = "Delete expense")
    @DeleteMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        financeService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    // ── CLIENT SUMMARY ───────────────────────────────────────────────────────

    @Operation(summary = "Get finance summary for a specific client")
    @GetMapping("/clients/{clientId}/summary")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<ClientFinanceSummaryResponse>> getClientSummary(
            @PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getClientSummary(clientId)));
    }

    // ── CASH FLOW ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get cash flow summary")
    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<CashFlowResponse>> getCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getCashFlow(from, to)));
    }

    // ── PURCHASES ────────────────────────────────────────────────────────────

    @Operation(summary = "List purchases")
    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseResponse>>> listPurchases(
            @RequestParam(required = false) PurchaseSource source,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("purchasedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                financeService.listPurchases(source, category, vendor, from, to, pageable))));
    }

    @Operation(summary = "Get purchase")
    @GetMapping("/purchases/{id}")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getPurchase(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getPurchase(id)));
    }

    @Operation(summary = "Create purchase")
    @PostMapping("/purchases")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> createPurchase(
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(financeService.createPurchase(request)));
    }

    @Operation(summary = "Update purchase")
    @PutMapping("/purchases/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updatePurchase(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.updatePurchase(id, request)));
    }

    @Operation(summary = "Delete purchase")
    @DeleteMapping("/purchases/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    public ResponseEntity<Void> deletePurchase(@PathVariable UUID id) {
        financeService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }

    // ── P&L REPORT ────────────────────────────────────────────────────────────

    @Operation(summary = "Get P&L report")
    @GetMapping("/reports/pnl")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<PnlReportResponse>> getPnlReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getPnlReport(from, to)));
    }
}
