package com.controltower.app.activity.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks user navigation and page views within Control Tower.
 * Used for employee activity monitoring and security auditing.
 */
@Entity
@Table(name = "user_activity",
       indexes = {
           @Index(name = "idx_activity_user_time", columnList = "userId, visitedAt DESC"),
           @Index(name = "idx_activity_session", columnList = "sessionId"),
           @Index(name = "idx_activity_route", columnList = "routePath"),
       })
@Getter
@Setter
public class UserActivity extends BaseEntity {

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

    /** Browser user agent. */
    @Column(name = "user_agent")
    private String userAgent;

    /** Client IP address. */
    @Column(name = "ip_address")
    private String ipAddress;

    /** When the user actually visited this page. */
    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;
}
