package com.controltower.app.activity.application;

import com.controltower.app.activity.api.dto.UserActivityRequest;
import com.controltower.app.activity.api.dto.UserActivityResponse;
import com.controltower.app.activity.domain.UserActivity;
import com.controltower.app.activity.domain.UserActivityRepository;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository activityRepository;
    private final TenantRepository       tenantRepository;
    private final UserRepository         userRepository;

    // ── Navigation tracking (called from frontend router) ────────────────────

    @Transactional
    public void recordActivity(Authentication auth, HttpServletRequest request, UserActivityRequest dto) {
        UUID tenantId = TenantContext.getTenantId();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", auth.getName()));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        UserActivity activity = new UserActivity();
        activity.setTenant(tenant);
        activity.setUserId(user.getId());
        activity.setUserName(user.getFullName());
        activity.setUserEmail(user.getEmail());
        activity.setEventType(UserActivity.EventType.NAVIGATION);
        activity.setRoutePath(dto.getRoutePath());
        activity.setPageTitle(dto.getPageTitle());
        activity.setDurationSeconds(dto.getDurationSeconds());
        activity.setFullUrl(dto.getFullUrl());
        activity.setSessionId(dto.getSessionId());
        activity.setVisitedAt(dto.getVisitedAt() != null ? dto.getVisitedAt() : Instant.now());
        activity.setUserAgent(request.getHeader("User-Agent"));
        activity.setIpAddress(extractIp(request));

        activityRepository.save(activity);
    }

    // ── Action tracking (called from UserActivityEventListener) ─────────────

    @Transactional
    public void recordAction(UUID tenantId, UUID userId,
                             String actionName, String entityType, String entityId,
                             String description, Map<String, Object> metadata,
                             Instant occurredAt) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        User user = userRepository.findById(userId).orElse(null);

        UserActivity activity = new UserActivity();
        activity.setTenant(tenant);
        activity.setUserId(userId);
        activity.setUserName(user != null ? user.getFullName() : null);
        activity.setUserEmail(user != null ? user.getEmail() : null);
        activity.setEventType(UserActivity.EventType.ACTION);
        activity.setActionName(actionName);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);
        activity.setDescription(description);
        activity.setMetadata(metadata);
        // routePath not applicable for action events — set to a placeholder
        activity.setRoutePath("/action/" + actionName.toLowerCase().replace('_', '/'));
        activity.setVisitedAt(occurredAt != null ? occurredAt : Instant.now());

        activityRepository.save(activity);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UserActivityResponse> query(
            UUID userId,
            UserActivity.EventType eventType,
            Instant from,
            Instant to,
            Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<UserActivity> page = activityRepository.findByFilters(tenantId, userId, eventType, from, to, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserActivityResponse> queryMyActivity(Authentication auth, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", auth.getName()));
        Page<UserActivity> page = activityRepository.findByFilters(
                tenantId, user.getId(), null, null, null, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long countActiveUsersSince(Instant since) {
        UUID tenantId = TenantContext.getTenantId();
        return activityRepository.findActiveSince(tenantId, since).stream()
                .map(UserActivity::getUserId)
                .distinct()
                .count();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserActivityResponse toResponse(UserActivity a) {
        return UserActivityResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userName(a.getUserName())
                .userEmail(a.getUserEmail())
                .eventType(a.getEventType().name())
                .actionName(a.getActionName())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .description(a.getDescription())
                .routePath(a.getRoutePath())
                .pageTitle(a.getPageTitle())
                .durationSeconds(a.getDurationSeconds())
                .fullUrl(a.getFullUrl())
                .sessionId(a.getSessionId())
                .ipAddress(a.getIpAddress())
                .visitedAt(a.getVisitedAt())
                .build();
    }

    private String extractIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
