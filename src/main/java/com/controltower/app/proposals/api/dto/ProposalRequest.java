package com.controltower.app.proposals.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProposalRequest(
        @NotNull UUID clientId,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate validityDate,
        @Size(max = 10) String currency,
        BigDecimal taxRate,
        String notes,
        String terms,
        @NotEmpty @Valid List<ProposalLineItemRequest> lineItems,
        @Size(max = 20) String discountType,
        BigDecimal discountValue
) {}
