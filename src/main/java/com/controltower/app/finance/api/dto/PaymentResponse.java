package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Payment.PaymentMethod;
import com.controltower.app.finance.domain.Payment.PaymentSource;
import com.controltower.app.finance.domain.RecurrenceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID tenantId,
        UUID clientId,
        String clientName,
        UUID invoiceId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String reference,
        String notes,
        Instant paidAt,
        Instant createdAt,
        PaymentSource source,
        String posReference,
        boolean isRecurring,
        RecurrenceType recurrenceType,
        LocalDate recurrenceEndDate,
        LocalDate nextOccurrenceDate,
        UUID parentRecurringId
) {}
