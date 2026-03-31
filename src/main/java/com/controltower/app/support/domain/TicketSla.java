package com.controltower.app.support.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SLA record for a ticket. One-to-one with Ticket.
 * SLA windows (response time by priority) are defined in application config.
 * Default: LOW=48h, MEDIUM=24h, HIGH=8h, CRITICAL=2h
 */
@Entity
@Table(name = "ticket_slas")
@Getter
@Setter
public class TicketSla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "breached_at")
    private Instant breachedAt;

    @Column(name = "breached", nullable = false)
    private boolean breached = false;

    public void markBreached() {
        this.breached = true;
        this.breachedAt = Instant.now();
    }
}
