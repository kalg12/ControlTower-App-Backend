package com.controltower.app.reminders.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientReminderHistoryRepository extends JpaRepository<ClientReminderHistory, UUID> {
    List<ClientReminderHistory> findByReminderIdOrderByCompletedAtDesc(UUID reminderId);
}