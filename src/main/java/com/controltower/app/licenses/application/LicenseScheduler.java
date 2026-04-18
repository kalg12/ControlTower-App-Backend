package com.controltower.app.licenses.application;

import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.licenses.domain.License;
import com.controltower.app.licenses.domain.LicenseRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseScheduler {

    private final LicenseService      licenseService;
    private final LicenseRepository   licenseRepository;
    private final NotificationService notificationService;
    private final UserRepository      userRepository;

    /** Daily at 02:00 UTC: move expired licenses into grace period. */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkExpiringLicenses() {
        log.info("Checking for expiring licenses");
        licenseService.processExpiring(Instant.now().plus(1, ChronoUnit.HOURS));
    }

    /** Daily at 02:05 UTC: suspend licenses whose grace period has ended. */
    @Scheduled(cron = "0 5 2 * * *")
    public void checkGraceExpired() {
        log.info("Checking for grace-expired licenses");
        licenseService.processGraceExpired();
    }

    /** Daily at 02:10 UTC: send LICENSE_EXPIRING_SOON at 30d, 7d, 1d thresholds. */
    @Scheduled(cron = "0 10 2 * * *")
    @Transactional
    public void checkExpiringSoon() {
        Instant now = Instant.now();
        notifyExpiringIn(now, 30, Notification.Severity.WARNING);
        notifyExpiringIn(now, 7, Notification.Severity.ERROR);
        notifyExpiringIn(now, 1, Notification.Severity.CRITICAL);
    }

    private void notifyExpiringIn(Instant now, int days, Notification.Severity severity) {
        Instant from = now.plus(days - 1, ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS);
        Instant to   = now.plus(days, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        List<License> licenses = licenseRepository.findExpiringBetween(from, to);
        for (License license : licenses) {
            List<UUID> admins = userRepository.findByTenantIdAndPermission(license.getTenantId(), "tenant:read")
                    .stream().map(u -> u.getId()).collect(Collectors.toList());
            if (admins.isEmpty()) continue;
            notificationService.send(
                    license.getTenantId(),
                    "LICENSE_EXPIRING_SOON",
                    "Licencia por vencer en " + days + " día(s)",
                    "La licencia del cliente vence en " + days + " día(s). Por favor renueve a tiempo.",
                    severity,
                    Map.of("licenseId", license.getId().toString(), "daysAway", days),
                    admins);
        }
    }
}
