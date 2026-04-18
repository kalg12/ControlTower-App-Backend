package com.controltower.app.finance.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
public class Expense extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ExpenseCategory category = ExpenseCategory.OTHER;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "MXN";

    @Column(name = "vendor", length = 200)
    private String vendor;

    @Column(name = "receipt_url", length = 1000)
    private String receiptUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt = Instant.now();

    public enum ExpenseCategory {
        PAYROLL, SERVICES, RENT, MARKETING, TECH, TRAVEL, SUPPLIES, TAXES, OTHER
    }
}
