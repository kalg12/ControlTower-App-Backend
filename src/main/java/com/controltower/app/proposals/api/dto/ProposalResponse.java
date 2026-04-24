package com.controltower.app.proposals.api.dto;

import com.controltower.app.proposals.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProposalResponse {

    private UUID id;
    private UUID tenantId;
    private UUID clientId;
    private String clientName;
    private String clientEmail;
    private String number;
    private String title;
    private String description;
    private ProposalStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String currency;
    private LocalDate validityDate;
    private String notes;
    private String terms;
    private Instant sentAt;
    private Instant viewedAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private UUID sentById;
    private Instant emailViewedAt;
    private List<ProposalLineItemResponse> lineItems;
    private Instant createdAt;
    private Instant updatedAt;
}
