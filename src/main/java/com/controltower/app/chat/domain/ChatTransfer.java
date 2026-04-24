package com.controltower.app.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_transfers")
@Getter
@Setter
public class ChatTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "from_agent_id", nullable = false)
    private UUID fromAgentId;

    @Column(name = "to_agent_id", nullable = false)
    private UUID toAgentId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "transferred_at", updatable = false)
    private Instant transferredAt;
}
