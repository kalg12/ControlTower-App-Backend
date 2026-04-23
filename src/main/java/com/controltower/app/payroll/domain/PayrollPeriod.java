package com.controltower.app.payroll.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payroll_periods")
@Getter
@Setter
public class PayrollPeriod extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PeriodStatus status = PeriodStatus.DRAFT;

    @Column(name = "total_gross", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollItem> items = new ArrayList<>();

    public void recalculateTotals() {
        totalGross = items.stream().map(PayrollItem::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalDeductions = items.stream().map(PayrollItem::getTotalDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalNet = items.stream().map(PayrollItem::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public enum PeriodType { MENSUAL, QUINCENAL }

    public enum PeriodStatus { DRAFT, PROCESSED, PAID }
}
