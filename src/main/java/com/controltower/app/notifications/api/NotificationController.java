package com.controltower.app.notifications.api;

import com.controltower.app.notifications.api.dto.NotificationResponse;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Notifications", description = "In-app real-time notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId  = UUID.fromString(principal.getUsername());
        PageRequest pageable = PageRequest.of(page, size, Sort.by("notification.createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(notificationService.listForUser(userId, pageable))));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        notificationService.markRead(id, UUID.fromString(principal.getUsername()));
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllRead(UUID.fromString(principal.getUsername()));
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        notificationService.delete(id, UUID.fromString(principal.getUsername()));
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted"));
    }
}
