package com.controltower.app.finance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClientFinanceSummaryResponse(
        UUID clientId,
        String clientName,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalOutstanding,
        BigDecimal totalExpenses,
        long invoiceCount,
        long paymentCount,
        long expenseCount,
        Instant lastInvoiceAt
) {}
