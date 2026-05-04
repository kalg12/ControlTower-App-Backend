package com.controltower.app.kanban.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MoveCardRequest {

    @NotNull(message = "targetColumnId is required")
    private UUID targetColumnId;

    private int position = 0;

    private boolean notifyByEmail = false;
}
