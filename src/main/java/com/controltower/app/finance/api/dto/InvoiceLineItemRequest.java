package com.controltower.app.finance.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvoiceLineItemRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        int position
) {}
