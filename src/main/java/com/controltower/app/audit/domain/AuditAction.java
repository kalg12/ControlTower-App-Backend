package com.controltower.app.audit.domain;

/**
 * Enumeration of all auditable actions in Control Tower.
 * Extend this enum when adding new sensitive operations.
 */
public enum AuditAction {

    // Identity
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    TOKEN_REFRESHED,

    // Tenant management
    TENANT_CREATED,
    TENANT_UPDATED,
    TENANT_SUSPENDED,
    TENANT_REACTIVATED,

    // User management
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,

    // Client management
    CLIENT_CREATED,
    CLIENT_UPDATED,
    CLIENT_DELETED,
    BRANCH_CREATED,
    BRANCH_DELETED,

    // Licensing
    LICENSE_ACTIVATED,
    LICENSE_SUSPENDED,
    LICENSE_REACTIVATED,
    LICENSE_CANCELLED,
    FEATURE_FLAG_CHANGED,

    // Support
    TICKET_CREATED,
    TICKET_ASSIGNED,
    TICKET_STATUS_CHANGED,
    TICKET_ESCALATED,
    TICKET_CLOSED,
    TICKET_DELETED,

    // Health
    HEALTH_RULE_CREATED,
    HEALTH_RULE_UPDATED,
    HEALTH_INCIDENT_OPENED,
    HEALTH_INCIDENT_RESOLVED,

    // Integrations
    INTEGRATION_CREATED,
    INTEGRATION_UPDATED,
    INTEGRATION_DELETED,
    SECRET_ROTATED,

    // Billing
    STRIPE_WEBHOOK_PROCESSED,
    SUBSCRIPTION_UPDATED,

    // Config
    CONFIG_CHANGED,

    // Generic
    CREATE,
    UPDATE,
    DELETE
}
