package com.controltower.app.finance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceLineItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        int position,
        Instant createdAt
) {}
