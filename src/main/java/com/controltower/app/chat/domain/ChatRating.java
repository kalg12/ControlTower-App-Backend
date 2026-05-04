package com.controltower.app.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_ratings")
@Getter
@Setter
public class ChatRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
