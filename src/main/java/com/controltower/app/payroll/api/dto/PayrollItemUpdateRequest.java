package com.controltower.app.payroll.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayrollItemUpdateRequest(
        @NotNull @DecimalMin("0") BigDecimal daysWorked,
        @NotNull @DecimalMin("0") BigDecimal overtimeHours,
        @NotNull @DecimalMin("0") BigDecimal otherDeductions,
        String notes
) {}
