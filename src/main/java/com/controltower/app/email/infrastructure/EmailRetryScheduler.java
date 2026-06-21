package com.controltower.app.email.infrastructure;

import com.controltower.app.email.application.EmailOutboundService;
import com.controltower.app.email.domain.EmailDelivery;
import com.controltower.app.email.domain.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Retries QUEUED email deliveries that are due for retry. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRetryScheduler {

    private final EmailDeliveryRepository deliveryRepo;
    private final EmailOutboundService outboundService;

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedDeliveries() {
        List<EmailDelivery> due = deliveryRepo.findDueForRetry(Instant.now());
        if (due.isEmpty()) return;

        log.info("Retrying {} email delivery(ies)", due.size());
        due.forEach(outboundService::attempt);
    }
}
