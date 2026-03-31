package com.controltower.app.licenses.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "licenses")
@Getter
@Setter
public class License extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false, unique = true)
    private UUID clientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LicenseStatus status = LicenseStatus.TRIAL;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart = Instant.now();

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "grace_period_end")
    private Instant gracePeriodEnd;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── Status transitions ────────────────────────────────────────────

    public void activate(Instant periodEnd) {
        this.status           = LicenseStatus.ACTIVE;
        this.currentPeriodEnd = periodEnd;
        this.gracePeriodEnd   = null;
    }

    public void suspend() {
        this.status = LicenseStatus.SUSPENDED;
    }

    public void reactivate(Instant newPeriodEnd) {
        this.status           = LicenseStatus.ACTIVE;
        this.currentPeriodEnd = newPeriodEnd;
        this.gracePeriodEnd   = null;
    }

    public void enterGracePeriod(Instant graceEnd) {
        this.status         = LicenseStatus.GRACE;
        this.gracePeriodEnd = graceEnd;
    }

    public void cancel() {
        this.status = LicenseStatus.CANCELLED;
    }

    public boolean isUsable() {
        return status == LicenseStatus.TRIAL
            || status == LicenseStatus.ACTIVE
            || status == LicenseStatus.GRACE;
    }

    public enum LicenseStatus { TRIAL, ACTIVE, GRACE, SUSPENDED, CANCELLED }
}
