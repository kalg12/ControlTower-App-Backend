package com.controltower.app.notifications.application;

import com.controltower.app.notifications.api.dto.NotificationResponse;
import com.controltower.app.notifications.domain.*;
import com.controltower.app.notifications.infrastructure.WebSocketNotificationChannel;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository           notificationRepository;
    private final NotificationUserStateRepository  stateRepository;
    private final WebSocketNotificationChannel     webSocketChannel;
    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Creates a notification and fans it out to all specified recipients.
     * Called internally from event listeners (no TenantContext required).
     */
    @Transactional
    public void send(UUID tenantId, String type, String title, String body,
                     Notification.Severity severity, Map<String, Object> metadata,
                     List<UUID> recipientUserIds) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setSeverity(severity);
        notification.setMetadata(metadata);
        notificationRepository.save(notification);

        List<UUID> filtered = recipientUserIds.stream()
                .filter(uid -> preferenceRepository.isEnabled(uid, type))
                .collect(Collectors.toList());

        filtered.forEach(userId -> {
            NotificationUserState state = new NotificationUserState();
            state.setNotification(notification);
            state.setUserId(userId);
            stateRepository.save(state);
            webSocketChannel.push(userId, toResponse(state));
        });
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForUser(UUID userId, Boolean onlyUnread, Pageable pageable) {
        if (Boolean.TRUE.equals(onlyUnread)) {
            return stateRepository.findUnreadPageByUserId(userId, pageable).map(this::toResponse);
        }
        return stateRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        NotificationUserState state = resolveState(notificationId, userId);
        state.markRead();
        stateRepository.save(state);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        stateRepository.findUnreadByUserId(userId).forEach(state -> {
            state.markRead();
            stateRepository.save(state);
        });
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        NotificationUserState state = resolveState(notificationId, userId);
        state.softDelete();
        stateRepository.save(state);
    }

    @Transactional
    public void deleteAll(UUID userId) {
        stateRepository.softDeleteAllByUserId(userId, java.time.Instant.now());
    }

    // ── Preferences ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationPreference> getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId);
    }

    @Transactional
    public NotificationPreference setPreference(UUID userId, String notificationType, boolean enabled) {
        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationType(userId, notificationType)
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setUserId(userId);
                    p.setNotificationType(notificationType);
                    return p;
                });
        pref.setEnabled(enabled);
        return preferenceRepository.save(pref);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private NotificationUserState resolveState(UUID notificationId, UUID userId) {
        return stateRepository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    }

    private NotificationResponse toResponse(NotificationUserState s) {
        Notification n = s.getNotification();
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .severity(n.getSeverity().name())
                .metadata(n.getMetadata())
                .read(s.getReadAt() != null)
                .readAt(s.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
