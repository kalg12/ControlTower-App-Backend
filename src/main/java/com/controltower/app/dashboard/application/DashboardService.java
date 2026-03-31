package com.controltower.app.dashboard.application;

import com.controltower.app.clients.domain.ClientBranchRepository;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.dashboard.api.dto.DashboardStats;
import com.controltower.app.health.domain.HealthCheck;
import com.controltower.app.health.domain.HealthCheckRepository;
import com.controltower.app.health.domain.HealthIncidentRepository;
import com.controltower.app.licenses.domain.License;
import com.controltower.app.licenses.domain.LicenseRepository;
import com.controltower.app.notifications.domain.NotificationUserStateRepository;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.support.domain.TicketSlaRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository          clientRepository;
    private final ClientBranchRepository    branchRepository;
    private final HealthCheckRepository     healthCheckRepository;
    private final HealthIncidentRepository  incidentRepository;
    private final TicketRepository          ticketRepository;
    private final TicketSlaRepository       slaRepository;
    private final LicenseRepository         licenseRepository;
    private final NotificationUserStateRepository notificationStateRepository;

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        UUID tenantId = TenantContext.getTenantId();

        // Clients & branches
        long totalClients   = clientRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long activeBranches = branchRepository.countByTenantIdAndDeletedAtIsNull(tenantId);

        // Health: count latest status per branch
        List<HealthCheck> latestChecks = healthCheckRepository.findLatestPerBranch(tenantId);
        long branchesUp       = latestChecks.stream().filter(h -> h.getStatus() == HealthCheck.HealthStatus.UP).count();
        long branchesDown     = latestChecks.stream().filter(h -> h.getStatus() == HealthCheck.HealthStatus.DOWN).count();
        long branchesDegraded = latestChecks.stream().filter(h -> h.getStatus() == HealthCheck.HealthStatus.DEGRADED).count();
        long openIncidents    = incidentRepository.countByTenantIdAndResolvedAtIsNull(tenantId);

        // Tickets
        long openTickets       = ticketRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, Ticket.TicketStatus.OPEN);
        long ticketsInProgress = ticketRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, Ticket.TicketStatus.IN_PROGRESS);
        long slaBreached       = slaRepository.countActiveBreachedByTenant(tenantId);

        // Licenses
        long activeLicenses  = licenseRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, License.LicenseStatus.ACTIVE);
        long trialLicenses   = licenseRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, License.LicenseStatus.TRIAL);
        long expiredLicenses = licenseRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, License.LicenseStatus.SUSPENDED)
                             + licenseRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, License.LicenseStatus.CANCELLED);

        // Unread notifications for the current user
        UUID currentUserId = getCurrentUserId();
        long unread = currentUserId != null
                ? notificationStateRepository.findUnreadByUserId(currentUserId).size()
                : 0L;

        return DashboardStats.builder()
                .totalClients(totalClients)
                .activeBranches(activeBranches)
                .branchesUp(branchesUp)
                .branchesDown(branchesDown)
                .branchesDegraded(branchesDegraded)
                .openIncidents(openIncidents)
                .openTickets(openTickets)
                .ticketsInProgress(ticketsInProgress)
                .slaBreachedTickets(slaBreached)
                .activeLicenses(activeLicenses)
                .trialLicenses(trialLicenses)
                .expiredLicenses(expiredLicenses)
                .unreadNotifications(unread)
                .build();
    }

    private UUID getCurrentUserId() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            return UUID.fromString(name);
        } catch (Exception e) {
            return null;
        }
    }
}
