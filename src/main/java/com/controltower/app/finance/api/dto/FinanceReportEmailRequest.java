package com.controltower.app.finance.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FinanceReportEmailRequest(
        @NotBlank @Email String toEmail,
        @NotNull Instant from,
        @NotNull Instant to
) {}
