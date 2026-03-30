package com.controltower.app.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all internal domain events.
 * Events are published via Spring's ApplicationEventPublisher and consumed
 * by listeners within the same JVM (in-process event bus).
 *
 * This keeps modules decoupled: e.g., Health module publishes
 * HealthIncidentOpenedEvent; Support module listens and creates a ticket.
 * When the system grows, these can be migrated to Kafka/RabbitMQ with
 * minimal changes to the event definitions.
 */
public abstract class DomainEvent {

    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();

    /** Unique identifier for this event instance. */
    public String getEventId() {
        return eventId;
    }

    /** Timestamp when the event occurred. */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** The event type identifier (e.g., "health.incident.opened"). */
    public abstract String getEventType();
}
