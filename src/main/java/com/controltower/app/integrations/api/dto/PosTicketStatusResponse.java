package com.controltower.app.integrations.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PosTicketStatusResponse {
    private UUID ctTicketId;
    private String status;
    private String priority;
    private UUID assigneeId;
    private Instant firstCommentAt;
    private int publicCommentCount;
}
