package com.controltower.app.templates.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "response_templates")
@Getter
@Setter
public class ResponseTemplate extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "category")
    private String category;

    /** Short keyword (e.g. "/pass-reset") to quick-insert in chat */
    @Column(name = "shortcut")
    private String shortcut;
}
