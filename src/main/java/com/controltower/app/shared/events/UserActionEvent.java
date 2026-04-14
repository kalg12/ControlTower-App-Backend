package com.controltower.app.shared.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain event published whenever a user performs a significant business action
 * (e.g. moves a Kanban card, sends a campaign, logs a CRM interaction).
 *
 * Consumed by {@code UserActivityEventListener} which persists the event to the
 * {@code user_activity} table so it appears in the unified Activity Feed alongside
 * navigation events.
 *
 * Usage:
 * <pre>{@code
 * publisher.publishEvent(UserActionEvent.builder()
 *     .tenantId(tenantId)
 *     .userId(userId)
 *     .actionName("CARD_MOVED")
 *     .entityType("KanbanCard")
 *     .entityId(cardId.toString())
 *     .description("Moved card 'Fix login bug' to Done")
 *     .build());
 * }</pre>
 */
public class UserActionEvent extends DomainEvent {

    private final UUID tenantId;
    private final UUID userId;

    /** Machine-readable action code — matches AuditAction enum names. */
    private final String actionName;

    /** Domain entity type (e.g. KanbanCard, Campaign, ClientInteraction). */
    private final String entityType;

    /** UUID of the affected entity as a String (nullable). */
    private final String entityId;

    /** Human-readable description shown in the Activity Feed. */
    private final String description;

    /** Optional structured metadata (serialised to JSONB). */
    private final Map<String, Object> metadata;

    private UserActionEvent(Builder b) {
        this.tenantId    = b.tenantId;
        this.userId      = b.userId;
        this.actionName  = b.actionName;
        this.entityType  = b.entityType;
        this.entityId    = b.entityId;
        this.description = b.description;
        this.metadata    = b.metadata;
    }

    @Override
    public String getEventType() { return "activity.user_action"; }

    public UUID   getTenantId()    { return tenantId; }
    public UUID   getUserId()      { return userId; }
    public String getActionName()  { return actionName; }
    public String getEntityType()  { return entityType; }
    public String getEntityId()    { return entityId; }
    public String getDescription() { return description; }
    public Map<String, Object> getMetadata() { return metadata; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID tenantId;
        private UUID userId;
        private String actionName;
        private String entityType;
        private String entityId;
        private String description;
        private Map<String, Object> metadata;

        public Builder tenantId(UUID v)                   { tenantId = v;    return this; }
        public Builder userId(UUID v)                     { userId = v;      return this; }
        public Builder actionName(String v)               { actionName = v;  return this; }
        public Builder entityType(String v)               { entityType = v;  return this; }
        public Builder entityId(UUID v)                   { entityId = v != null ? v.toString() : null; return this; }
        public Builder entityId(String v)                 { entityId = v;    return this; }
        public Builder description(String v)              { description = v; return this; }
        public Builder metadata(Map<String, Object> v)    { metadata = v;    return this; }
        public UserActionEvent build()                    { return new UserActionEvent(this); }
    }
}
