package com.controltower.app.reminders.application;

import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.reminders.domain.ClientReminder;
import com.controltower.app.reminders.domain.ClientReminderHistory;
import com.controltower.app.reminders.domain.ClientReminderHistoryRepository;
import com.controltower.app.reminders.domain.ClientReminderRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientReminderService {

    private final ClientReminderRepository reminderRepository;
    private final ClientReminderHistoryRepository historyRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Page<ClientReminder> listReminders(UUID clientId, ClientReminder.ReminderStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        
        if (status != null) {
            return reminderRepository.findByTenantAndStatus(tenantId, status, pageable);
        }
        
        return reminderRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ClientReminder> getActiveRemindersForClient(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        return reminderRepository.findActiveByClient(tenantId, clientId);
    }

    @Transactional
    public ClientReminder createReminder(ClientReminder reminder, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        reminder.setTenantId(tenantId);
        reminder.setCreatedBy(userId);
        reminder.setStatus(ClientReminder.ReminderStatus.ACTIVE);
        reminder.setOccurrencesCount(0);
        
        if (reminder.getStartDate() == null) {
            reminder.setStartDate(Instant.now());
        }
        if (reminder.getNextDueDate() == null) {
            reminder.setNextDueDate(reminder.getStartDate());
        }
        
        return reminderRepository.save(reminder);
    }

    @Transactional
    public ClientReminder updateReminder(UUID id, ClientReminder updates) {
        UUID tenantId = TenantContext.getTenantId();
        ClientReminder existing = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getRecurrenceType() != null) existing.setRecurrenceType(updates.getRecurrenceType());
        if (updates.getRecurrenceDays() != null) existing.setRecurrenceDays(updates.getRecurrenceDays());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        if (updates.getNotifyUserIds() != null) existing.setNotifyUserIds(updates.getNotifyUserIds());
        
        return reminderRepository.save(existing);
    }

    @Transactional
    public void completeReminder(UUID id, UUID userId, String notes) {
        ClientReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        
        // Create history record
        ClientReminderHistory history = new ClientReminderHistory();
        history.setReminderId(id);
        history.setCompletedBy(userId);
        history.setNotes(notes);
        history.setOutcome(ClientReminderHistory.Outcome.COMPLETED);
        historyRepository.save(history);
        
        // Update reminder
        reminder.setLastCompletedDate(Instant.now());
        
        // Calculate next due date if still active
        if (reminder.getStatus() == ClientReminder.ReminderStatus.ACTIVE) {
            reminder.calculateNextDueDate();
            
            // Check if max occurrences reached
            if (reminder.getMaxOccurrences() != null && 
                reminder.getOccurrencesCount() >= reminder.getMaxOccurrences()) {
                reminder.setStatus(ClientReminder.ReminderStatus.COMPLETED);
            }
        }
        
        reminderRepository.save(reminder);
    }

    @Transactional
    public void snoozeReminder(UUID id, UUID userId, int days, String notes) {
        ClientReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        
        // Create history record
        ClientReminderHistory history = new ClientReminderHistory();
        history.setReminderId(id);
        history.setCompletedBy(userId);
        history.setNotes(notes);
        history.setOutcome(ClientReminderHistory.Outcome.SNOOZED);
        historyRepository.save(history);
        
        // Snooze: add days to next due date
        reminder.setNextDueDate(Instant.now().plus(days * 86400L, ChronoUnit.SECONDS));
        reminderRepository.save(reminder);
    }

    @Transactional
    public void pauseReminder(UUID id) {
        ClientReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        reminder.setStatus(ClientReminder.ReminderStatus.PAUSED);
        reminderRepository.save(reminder);
    }

    @Transactional
    public void resumeReminder(UUID id) {
        ClientReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        reminder.setStatus(ClientReminder.ReminderStatus.ACTIVE);
        reminderRepository.save(reminder);
    }

    @Transactional
    public void deleteReminder(UUID id) {
        ClientReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClientReminder", id));
        reminderRepository.delete(reminder);
    }

    @Transactional(readOnly = true)
    public List<ClientReminderHistory> getHistory(UUID reminderId) {
        return historyRepository.findByReminderIdOrderByCompletedAtDesc(reminderId);
    }

    public List<ClientReminder> findDueReminders() {
        return reminderRepository.findDueReminders(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    public String getClientName(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(c -> c.getName())
                .orElse(null);
    }
}