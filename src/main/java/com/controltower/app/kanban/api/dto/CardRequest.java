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
public class CardRequest {

    @NotNull(message = "columnId is required")
    private UUID columnId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private UUID assigneeId;
    private LocalDate dueDate;
    private Card.Priority priority = Card.Priority.MEDIUM;
    private int position = 0;
}
