package com.controltower.app.kanban.api;

import com.controltower.app.kanban.api.dto.WorkItemResponse;
import com.controltower.app.kanban.application.BoardService;
import com.controltower.app.kanban.domain.BoardColumn;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Cross-board work items live under {@code /api/v1/kanban} so they are not mistaken for
 * {@code GET /api/v1/boards/{id}} (Spring would try to parse {@code "work-items"} as a UUID).
 */
@Tag(name = "Kanban", description = "Kanban boards, columns, cards and checklists")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/kanban")
@RequiredArgsConstructor
public class KanbanWorkItemsController {

    private final BoardService boardService;

    @Operation(summary = "Cross-board work items", description = "Lists cards from all boards in the tenant, optionally filtered by assignee and default column kind (TODO, IN_PROGRESS, DONE, HISTORY). Requires the 'kanban:read' permission.")
    @GetMapping("/work-items")
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<List<WorkItemResponse>>> workItems(
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String columnKind) {
        BoardColumn.ColumnKind kind = null;
        if (columnKind != null && !columnKind.isBlank()) {
            try {
                kind = BoardColumn.ColumnKind.valueOf(columnKind.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ControlTowerException(
                        "Invalid columnKind. Use TODO, IN_PROGRESS, DONE, or HISTORY.", HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(boardService.listWorkItems(assigneeId, kind)));
    }
}
