package com.controltower.app.notifications.infrastructure;

import com.controltower.app.notifications.api.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pushes notifications to individual users via WebSocket/STOMP.
 * Client subscribes to /user/queue/notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotificationChannel {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(UUID userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }
}
