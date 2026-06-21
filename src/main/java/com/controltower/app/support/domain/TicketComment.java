package com.controltower.app.support.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_comments")
@Getter
@Setter
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Internal notes are visible only to agents, not to the client. */
    @Column(name = "is_internal", nullable = false)
    private boolean internal = false;

    /** Origin of this comment: AGENT (default), EMAIL (inbound), SYSTEM (automated). */
    @Column(name = "source", nullable = false)
    private String source = "AGENT";

    /** Reference to the raw inbound email that generated this comment (when source=EMAIL). */
    @Column(name = "email_raw_id")
    private java.util.UUID emailRawId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
