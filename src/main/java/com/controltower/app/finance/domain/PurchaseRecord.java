package com.controltower.app.finance.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_records")
@Getter
@Setter
public class PurchaseRecord extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vendor", length = 200)
    private String vendor;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "MXN";

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Expense.ExpenseCategory category = Expense.ExpenseCategory.OTHER;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "receipt_url", length = 1000)
    private String receiptUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private PurchaseSource source = PurchaseSource.MANUAL;

    @Column(name = "pos_reference", length = 200)
    private String posReference;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", length = 20)
    private RecurrenceType recurrenceType;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "next_occurrence_date")
    private LocalDate nextOccurrenceDate;

    @Column(name = "parent_recurring_id")
    private UUID parentRecurringId;

    public enum PurchaseSource {
        MANUAL, POS_IMPORT
    }
}
