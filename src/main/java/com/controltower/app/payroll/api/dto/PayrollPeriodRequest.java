package com.controltower.app.payroll.api.dto;

import com.controltower.app.payroll.domain.PayrollPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PayrollPeriodRequest(
        @NotNull @Min(2020) @Max(2099) Integer year,
        @NotNull @Min(1) @Max(24) Integer periodNumber,
        @NotNull PayrollPeriod.PeriodType periodType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String notes
) {}
