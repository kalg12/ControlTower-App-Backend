package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
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
        Instant createdAt
) {}
