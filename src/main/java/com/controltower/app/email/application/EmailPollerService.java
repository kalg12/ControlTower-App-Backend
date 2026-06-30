package com.controltower.app.email.application;

import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.email.domain.EmailMailboxConfigRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Polls all active IMAP mailboxes on a fixed schedule.
 * Each mailbox tracks its own poll interval; we check every 30s which ones are due.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPollerService {

    private final EmailMailboxConfigRepository mailboxRepo;
    private final ImapFetcherService imapFetcher;
    private final EmailProcessorService emailProcessor;

    private static final int MAX_ERRORS_BEFORE_DISABLE = 5;

    @Scheduled(fixedDelay = 30_000)
    public void pollDueMailboxes() {
        Instant now = Instant.now();

        List<EmailMailboxConfig> dueMailboxes = mailboxRepo.findDueForPoll(now).stream()
                .filter(m -> m.getLastPolledAt() == null
                        || m.getLastPolledAt().isBefore(now.minusSeconds(m.getPollIntervalSec())))
                .toList();

        if (dueMailboxes.isEmpty()) return;

        log.debug("Polling {} due mailbox(es)", dueMailboxes.size());

        for (EmailMailboxConfig config : dueMailboxes) {
            pollMailbox(config, now);
        }
    }

    private void pollMailbox(EmailMailboxConfig config, Instant now) {
        try {
            List<MimeMessage> messages = imapFetcher.fetchUnseen(config);

            for (MimeMessage message : messages) {
                emailProcessor.processAsync(config, message);
            }

            config.setLastPolledAt(now);
            config.setErrorCount(0);
            config.setLastError(null);

        } catch (Exception e) {
            log.error("IMAP poll failed for mailbox {} (tenant {}): {}",
                config.getImapUsername(), config.getTenantId(), e.getMessage());

            config.setLastError(e.getMessage());
            config.incrementErrorCount();
            config.setLastPolledAt(now);

            if (config.getErrorCount() >= MAX_ERRORS_BEFORE_DISABLE) {
                log.warn("Disabling mailbox {} after {} consecutive errors",
                    config.getImapUsername(), config.getErrorCount());
                config.setActive(false);
            }
        }

        mailboxRepo.save(config);
    }
}
