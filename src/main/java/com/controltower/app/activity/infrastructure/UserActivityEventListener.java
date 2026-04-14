package com.controltower.app.activity.infrastructure;

import com.controltower.app.activity.application.UserActivityService;
import com.controltower.app.shared.events.UserActionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous listener that persists {@link UserActionEvent}s to the
 * {@code user_activity} table so they appear in the unified Activity Feed.
 *
 * Running async ensures that the originating transaction is already committed
 * before we write the activity record, avoiding cross-transaction side effects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

    private final UserActivityService activityService;

    @Async
    @EventListener
    public void onUserAction(UserActionEvent event) {
        try {
            activityService.recordAction(
                    event.getTenantId(),
                    event.getUserId(),
                    event.getActionName(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getDescription(),
                    event.getMetadata(),
                    event.getOccurredAt());
        } catch (Exception ex) {
            // Activity tracking must never break the originating request
            log.warn("Failed to record activity for action={} entity={}: {}",
                    event.getActionName(), event.getEntityId(), ex.getMessage());
        }
    }
}
