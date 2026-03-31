package com.controltower.app.kanban.api.dto;

import com.controltower.app.kanban.domain.Board;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardRequest {

    @NotBlank(message = "Board name is required")
    private String name;

    private String description;

    private Board.Visibility visibility = Board.Visibility.TEAM;
}
