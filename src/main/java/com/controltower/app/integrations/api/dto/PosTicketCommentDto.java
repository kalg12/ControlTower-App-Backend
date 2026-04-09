package com.controltower.app.integrations.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PosTicketCommentDto {
    private UUID id;
    private String content;
    /** "OPERATOR" for CT agents, "POS_USER" for messages originating from the POS. */
    private String senderType;
    private Instant createdAt;
}
