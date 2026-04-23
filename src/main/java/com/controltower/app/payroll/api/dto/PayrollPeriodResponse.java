package com.controltower.app.payroll.api.dto;

import com.controltower.app.payroll.domain.PayrollPeriod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PayrollPeriodResponse {
    private UUID id;
    private UUID tenantId;
    private Integer year;
    private Integer periodNumber;
    private PayrollPeriod.PeriodType periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private PayrollPeriod.PeriodStatus status;
    private BigDecimal totalGross;
    private BigDecimal totalDeductions;
    private BigDecimal totalNet;
    private String notes;
    private Instant createdAt;
    private List<PayrollItemResponse> items;

    public static PayrollPeriodResponse from(PayrollPeriod p, List<PayrollItemResponse> items) {
        return PayrollPeriodResponse.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .year(p.getYear())
                .periodNumber(p.getPeriodNumber())
                .periodType(p.getPeriodType())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .totalGross(p.getTotalGross())
                .totalDeductions(p.getTotalDeductions())
                .totalNet(p.getTotalNet())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .items(items)
                .build();
    }

    public static PayrollPeriodResponse fromNoItems(PayrollPeriod p) {
        return from(p, List.of());
    }
}
