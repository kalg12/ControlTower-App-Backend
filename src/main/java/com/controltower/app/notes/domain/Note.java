package com.controltower.app.notes.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Entity type this note is linked to: CLIENT, TICKET, BRANCH, etc. */
    @Column(name = "linked_to")
    private String linkedTo;

    /** UUID of the linked entity. */
    @Column(name = "linked_id")
    private UUID linkedId;
}
