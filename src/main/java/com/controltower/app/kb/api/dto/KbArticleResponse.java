package com.controltower.app.kb.api.dto;

import com.controltower.app.kb.domain.KbArticle;
import com.controltower.app.kb.domain.KbStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Schema(description = "Knowledge base article")
@Getter
@Builder
public class KbArticleResponse {

    private final UUID    id;
    private final UUID    tenantId;
    private final UUID    authorId;
    private final String  title;
    private final String  content;
    private final String  category;
    private final List<String> tags;
    private final KbStatus status;
    private final int     views;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static KbArticleResponse from(KbArticle a) {
        List<String> tags = (a.getTagsCsv() == null || a.getTagsCsv().isBlank())
            ? Collections.emptyList()
            : Arrays.asList(a.getTagsCsv().split(","));

        return KbArticleResponse.builder()
            .id(a.getId())
            .tenantId(a.getTenantId())
            .authorId(a.getAuthorId())
            .title(a.getTitle())
            .content(a.getContent())
            .category(a.getCategory())
            .tags(tags)
            .status(a.getStatus())
            .views(a.getViews())
            .createdAt(a.getCreatedAt())
            .updatedAt(a.getUpdatedAt())
            .build();
    }
}
