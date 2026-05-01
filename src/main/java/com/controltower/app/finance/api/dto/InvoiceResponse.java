package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Invoice.InvoiceStatus;
import com.controltower.app.finance.domain.RecurrenceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID clientId,
        String clientName,
        String clientTaxId,
        String number,
        InvoiceStatus status,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        String currency,
        String notes,
        LocalDate issuedAt,
        LocalDate dueDate,
        Instant paidAt,
        List<InvoiceLineItemResponse> lineItems,
        Instant createdAt,
        Instant updatedAt,
        boolean isRecurring,
        RecurrenceType recurrenceType,
        LocalDate recurrenceEndDate,
        LocalDate nextOccurrenceDate,
        UUID parentRecurringId
) {}
