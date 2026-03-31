package com.controltower.app.support.infrastructure;

import com.controltower.app.support.domain.TicketSla;
import com.controltower.app.support.domain.TicketSlaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final TicketSlaRepository slaRepository;

    /** Every 5 minutes: mark SLAs whose due_at has passed. */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void checkBreaches() {
        List<TicketSla> overdue = slaRepository.findBreachedUnmarked(Instant.now());
        if (overdue.isEmpty()) return;

        log.warn("Marking {} SLA breach(es)", overdue.size());
        overdue.forEach(sla -> {
            sla.markBreached();
            slaRepository.save(sla);
        });
    }
}
