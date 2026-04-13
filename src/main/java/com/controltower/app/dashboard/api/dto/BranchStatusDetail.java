package com.controltower.app.dashboard.api.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Compact representation of a branch whose latest health check is not UP.
 * Included in DashboardStats.alertBranches so the frontend can name the affected branches.
 */
@Value
@Builder
public class BranchStatusDetail {
    UUID   branchId;
    String branchName;
    String clientName;
    String status;   // "DOWN" | "DEGRADED"
}
