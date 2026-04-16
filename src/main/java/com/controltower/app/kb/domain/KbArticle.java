package com.controltower.app.kb.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "kb_articles")
@Getter
@Setter
public class KbArticle extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category")
    private String category;

    /**
     * Comma-separated tag list (e.g. "billing,setup,pos").
     * Stored as TEXT in the DB; mapped to/from a string in the DTO layer.
     */
    @Column(name = "tags_csv")
    private String tagsCsv;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KbStatus status = KbStatus.DRAFT;

    @Column(name = "views", nullable = false)
    private int views = 0;
}
