package com.controltower.app.dashboard.api.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Summary statistics for the dashboard home screen.
 */
@Value
@Builder
public class DashboardStats {

    // Clients & branches
    long totalClients;
    long activeBranches;

    // Health
    long branchesUp;
    long branchesDown;
    long branchesDegraded;
    long openIncidents;

    // Support
    long openTickets;
    long ticketsInProgress;
    long slaBreachedTickets;

    // Licenses
    long activeLicenses;
    long trialLicenses;
    long expiredLicenses;

    // Notifications (current user)
    long unreadNotifications;
}
