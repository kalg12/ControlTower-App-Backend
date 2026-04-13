package com.controltower.app.activity.api;

import com.controltower.app.activity.api.dto.UserActivityRequest;
import com.controltower.app.activity.api.dto.UserActivityResponse;
import com.controltower.app.activity.application.UserActivityService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "User Activity", description = "Employee navigation and activity tracking")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService activityService;

    @Operation(summary = "Record page view", description = "Called by frontend router when user navigates to a new page. Requires 'activity:write'.")
    @PostMapping("/track")
    @PreAuthorize("hasAuthority('activity:write')")
    public ResponseEntity<ApiResponse<Void>> track(
            @Valid @RequestBody UserActivityRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        activityService.recordActivity(auth, httpRequest, request);
        return ResponseEntity.ok(ApiResponse.ok("Activity recorded"));
    }

    @Operation(summary = "Query all activity", description = "Paginated activity log with filters. Requires 'activity:read'.")
    @GetMapping
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserActivityResponse>>> query(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("visitedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(activityService.query(userId, from, to, pageable)));
    }

    @Operation(summary = "My activity", description = "Returns the current user's own navigation history. No special permission required.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<UserActivityResponse>>> myActivity(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("visitedAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(activityService.queryMyActivity(auth, pageable)));
    }

    @Operation(summary = "Active users count", description = "Count of unique users active in the last 15 minutes. Requires 'activity:read'.")
    @GetMapping("/active-users")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<ApiResponse<Long>> activeUsers() {
        Instant since = Instant.now().minusSeconds(15 * 60);
        return ResponseEntity.ok(ApiResponse.ok(activityService.countActiveUsersSince(since)));
    }
}
