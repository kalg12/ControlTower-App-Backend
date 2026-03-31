package com.controltower.app.notes.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NoteRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    /** e.g. "CLIENT", "TICKET", "BRANCH" */
    private String linkedTo;
    private UUID   linkedId;
}
