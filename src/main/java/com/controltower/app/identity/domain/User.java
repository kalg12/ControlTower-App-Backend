package com.controltower.app.identity.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A user belongs to a tenant and has one or more roles.
 * Super-admins (isSuperAdmin = true) can access all tenants and have no tenant restriction.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "email"})
)
@Getter
@Setter
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** Super-admins bypass tenant isolation and have all permissions. */
    @Column(name = "is_super_admin", nullable = false)
    private boolean superAdmin = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "kanban_points", nullable = false)
    private int kanbanPoints = 0;

    @Column(name = "overdue_attended", nullable = false)
    private int overdueAttended = 0;

    @Column(name = "google_refresh_token", columnDefinition = "TEXT")
    private String googleRefreshToken;

    @Column(name = "google_calendar_enabled", nullable = false)
    private boolean googleCalendarEnabled = false;

    @Column(name = "google_calendar_email")
    private String googleCalendarEmail;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Returns all permission codes from assigned roles. */
    public Set<String> getAllPermissions() {
        Set<String> perms = new HashSet<>();
        roles.forEach(r -> r.getPermissions().forEach(p -> perms.add(p.getCode())));
        return perms;
    }

    public void addKanbanPoints(int points) {
        this.kanbanPoints += points;
    }

    public void incrementOverdueAttended() {
        this.overdueAttended++;
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, PENDING_VERIFICATION
    }
}
