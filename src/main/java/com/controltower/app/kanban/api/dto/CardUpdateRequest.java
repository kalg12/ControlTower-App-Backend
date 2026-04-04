package com.controltower.app.kanban.api.dto;

import com.controltower.app.kanban.domain.Card;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CardUpdateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private UUID assigneeId;

    private LocalDate dueDate;

    @NotNull(message = "Priority is required")
    private Card.Priority priority;
}
