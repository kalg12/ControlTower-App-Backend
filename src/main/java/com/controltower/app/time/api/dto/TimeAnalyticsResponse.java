package com.controltower.app.time.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TimeAnalyticsResponse {

    /** Average ticket resolution time in minutes (only resolved/closed tickets with time entries). */
    private final double avgResolutionMinutes;

    /** Percentage of tickets whose SLA was not breached (0-100). */
    private final double slaComplianceRate;

    /** Total time entries count in period. */
    private final long totalEntries;

    /** Total minutes logged in period. */
    private final long totalLoggedMinutes;

    /** Top users by minutes logged in the period. */
    private final List<UserTimeEntry> topUsers;

    @Getter
    @Builder
    public static class UserTimeEntry {
        private final UUID   userId;
        private final long   totalMinutes;
    }
}
