package com.controltower.app.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "visitor_id", nullable = false, length = 100)
    private String visitorId;

    @Column(name = "visitor_token", nullable = false, unique = true)
    private UUID visitorToken;

    @Column(name = "visitor_name", nullable = false, length = 255)
    private String visitorName;

    @Column(name = "visitor_email", length = 255)
    private String visitorEmail;

    @Column(name = "agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.WAITING;

    @Column(name = "source", nullable = false, length = 50)
    private String source = "POS";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();
}
