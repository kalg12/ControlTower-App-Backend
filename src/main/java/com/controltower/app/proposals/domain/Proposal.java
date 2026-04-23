package com.controltower.app.proposals.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "proposals")
@Getter
@Setter
public class Proposal extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "number", nullable = false, length = 50)
    private String number;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("16.00");

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "MXN";

    @Column(name = "validity_date")
    private LocalDate validityDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "sent_by_id")
    private UUID sentById;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<ProposalLineItem> lineItems = new ArrayList<>();

    public void recalculate() {
        this.subtotal  = lineItems.stream().map(ProposalLineItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        this.total     = subtotal.add(taxAmount);
    }
}
