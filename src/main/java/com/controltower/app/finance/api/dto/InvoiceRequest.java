package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.RecurrenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        UUID clientId,
        @Size(max = 10) String currency,
        @DecimalMin("0.00") BigDecimal taxRate,
        @Size(max = 5000) String notes,
        LocalDate issuedAt,
        LocalDate dueDate,
        @NotEmpty @Valid List<InvoiceLineItemRequest> lineItems,
        Boolean isRecurring,
        RecurrenceType recurrenceType,
        LocalDate recurrenceEndDate
) {}
