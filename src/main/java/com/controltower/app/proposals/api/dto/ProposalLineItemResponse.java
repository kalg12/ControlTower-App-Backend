package com.controltower.app.proposals.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProposalLineItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        int position,
        Instant createdAt
) {}
