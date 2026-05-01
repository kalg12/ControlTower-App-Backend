package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Expense.ExpenseCategory;
import com.controltower.app.finance.domain.PurchaseRecord.PurchaseSource;
import com.controltower.app.finance.domain.RecurrenceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        UUID tenantId,
        String vendor,
        String description,
        BigDecimal amount,
        String currency,
        ExpenseCategory category,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String receiptUrl,
        String notes,
        Instant purchasedAt,
        PurchaseSource source,
        String posReference,
        boolean isRecurring,
        RecurrenceType recurrenceType,
        LocalDate recurrenceEndDate,
        LocalDate nextOccurrenceDate,
        UUID parentRecurringId,
        Instant createdAt,
        Instant updatedAt
) {}
