package com.controltower.app.support.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TicketCommentResponse {
    private UUID id;
    private UUID authorId;
    /** Full name of the author (null for POS_USER origin). */
    private String authorName;
    private String content;
    private boolean internal;
    /** "OPERATOR" when authorId is non-null (CT agent), "POS_USER" when authorId is null (POS origin). */
    private String senderType;
    private Instant createdAt;
}
