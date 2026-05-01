package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Expense.ExpenseCategory;
import com.controltower.app.finance.domain.PurchaseRecord.PurchaseSource;
import com.controltower.app.finance.domain.RecurrenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PurchaseRequest(
        @Size(max = 200) String vendor,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 10) String currency,
        ExpenseCategory category,
        @DecimalMin("0.01") BigDecimal quantity,
        @DecimalMin("0.00") BigDecimal unitPrice,
        @Size(max = 1000) String receiptUrl,
        String notes,
        Instant purchasedAt,
        PurchaseSource source,
        @Size(max = 200) String posReference,
        Boolean isRecurring,
        RecurrenceType recurrenceType,
        LocalDate recurrenceEndDate
) {}
