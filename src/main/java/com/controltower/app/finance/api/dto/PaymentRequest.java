package com.controltower.app.finance.api.dto;

import com.controltower.app.finance.domain.Payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequest(
        UUID clientId,
        UUID invoiceId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 10) String currency,
        PaymentMethod method,
        @Size(max = 200) String reference,
        String notes,
        Instant paidAt
) {}
