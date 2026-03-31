package com.controltower.app.licenses.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseScheduler {

    private final LicenseService licenseService;

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
}
