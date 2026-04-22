package com.controltower.app.calendar.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
public class CalendarEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.MEETING;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status = EventStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_channel")
    private ContactChannel contactChannel;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "calendar_event_assignees", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "user_id")
    private List<UUID> assigneeIds = new ArrayList<>();

    @Column(name = "google_event_id")
    private String googleEventId;

    public enum EventType {
        CALL, MEETING, SITE_VISIT, DEMO, FOLLOW_UP, WHATSAPP, INSTAGRAM, OTHER
    }

    public enum EventStatus {
        SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    }

    public enum ContactChannel {
        WHATSAPP, INSTAGRAM, FACEBOOK, EMAIL, PHONE, IN_PERSON
    }
}
