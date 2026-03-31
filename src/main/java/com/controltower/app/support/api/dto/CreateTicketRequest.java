package com.controltower.app.support.api.dto;

import com.controltower.app.support.domain.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTicketRequest {

    private UUID clientId;
    private UUID branchId;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    private String description;

    private Ticket.Priority priority;
}
