package com.controltower.app.activity.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks all user activity within Control Tower.
 *
 * Two event types are stored in this single table:
 *   NAVIGATION — page-view events sent by the frontend router after each navigation.
 *   ACTION     — business-action events emitted by backend services via UserActionEvent
 *                (e.g. CARD_MOVED, CAMPAIGN_SENT, INTERACTION_LOGGED).
 */
@Entity
@Table(name = "user_activity",
       indexes = {
           @Index(name = "idx_activity_user_time",        columnList = "user_id, visited_at DESC"),
           @Index(name = "idx_activity_session",          columnList = "session_id"),
           @Index(name = "idx_activity_route",            columnList = "route_path"),
           @Index(name = "idx_activity_visited",          columnList = "visited_at DESC"),
           @Index(name = "idx_activity_tenant_time",      columnList = "tenant_id, visited_at DESC"),
       })
@Getter
@Setter
public class UserActivity extends BaseEntity {

    // ── Who ─────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "session_id")
    private String sessionId;

    // ── Event classification ─────────────────────────────────────────────────

    /** NAVIGATION = page view from router; ACTION = backend business event. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.NAVIGATION;

    /** For ACTION events: machine-readable action code (e.g. CARD_MOVED). */
    @Column(name = "action_name")
    private String actionName;

    /** For ACTION events: domain entity type (e.g. KanbanCard, Campaign). */
    @Column(name = "entity_type")
    private String entityType;

    /** For ACTION events: UUID of the affected entity. */
    @Column(name = "entity_id")
    private String entityId;

    /** Human-readable description (e.g. "Moved card 'Fix login' to Done"). */
    @Column(name = "description")
    private String description;

    /** Optional structured metadata for the event (stored as JSONB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    // ── Navigation-specific ──────────────────────────────────────────────────

    /** The route/path the user navigated to (e.g. /clients, /tickets/abc-123). */
    @Column(name = "route_path", nullable = false)
    private String routePath;

    /** Human-readable page title (e.g. "Clients", "Ticket Detail"). */
    @Column(name = "page_title")
    private String pageTitle;

    /** How long (seconds) the user stayed on this page before navigating away. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** Full URL including query params. */
    @Column(name = "full_url")
    private String fullUrl;

    // ── Context ──────────────────────────────────────────────────────────────

    /** Browser user agent. */
    @Column(name = "user_agent")
    private String userAgent;

    /** Client IP address. */
    @Column(name = "ip_address")
    private String ipAddress;

    /** When the event occurred (page visited or action performed). */
    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    // ── Event type enum ──────────────────────────────────────────────────────

    public enum EventType {
        /** Frontend router navigation — page view with optional duration. */
        NAVIGATION,
        /** Backend business action — card move, campaign send, etc. */
        ACTION
    }
}
