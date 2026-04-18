package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Expense.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseRequest(
        UUID clientId,
        ExpenseCategory category,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 10) String currency,
        @Size(max = 200) String vendor,
        @Size(max = 1000) String receiptUrl,
        String notes,
        Instant paidAt
) {}
