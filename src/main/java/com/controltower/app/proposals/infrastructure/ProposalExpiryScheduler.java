package com.controltower.app.proposals.infrastructure;

import com.controltower.app.proposals.domain.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalExpiryScheduler {

    private final ProposalRepository proposalRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireStaleProposals() {
        int count = proposalRepository.bulkExpire(LocalDate.now());
        if (count > 0) {
            log.info("Expired {} proposal(s) past their validity date", count);
        }
    }
}
