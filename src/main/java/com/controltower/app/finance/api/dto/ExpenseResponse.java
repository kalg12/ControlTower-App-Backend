package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID tenantId,
        ExpenseCategory category,
        String description,
        BigDecimal amount,
        String currency,
        String vendor,
        String receiptUrl,
        String notes,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {}
