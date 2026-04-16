package com.controltower.app.finance.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "MXN";

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Column(name = "reference", length = 200)
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt = Instant.now();

    public enum PaymentMethod {
        BANK_TRANSFER, CASH, CARD, CHECK, CRYPTO, OTHER
    }
}
