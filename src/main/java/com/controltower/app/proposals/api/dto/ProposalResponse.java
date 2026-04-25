package com.controltower.app.proposals.api.dto;

import com.controltower.app.proposals.domain.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProposalResponse(
    UUID id,
    UUID tenantId,
    UUID clientId,
    String clientName,
    String clientEmail,
    String number,
    String title,
    String description,
    ProposalStatus status,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal total,
    String currency,
    LocalDate validityDate,
    String notes,
    String terms,
    Instant sentAt,
    Instant viewedAt,
    Instant acceptedAt,
    Instant rejectedAt,
    UUID sentById,
    Instant emailViewedAt,
    List<ProposalLineItemResponse> lineItems,
    Instant createdAt,
    Instant updatedAt
) {}