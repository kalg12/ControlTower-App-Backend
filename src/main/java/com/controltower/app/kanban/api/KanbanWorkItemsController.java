package com.controltower.app.kanban.api;

import com.controltower.app.kanban.api.dto.WorkItemResponse;
import com.controltower.app.kanban.application.BoardService;
import com.controltower.app.kanban.domain.BoardColumn;
import com.controltower.app.kanban.domain.Card;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "Cross-board work items", description = "Lists cards from all boards in the tenant, optionally filtered by boardId, assignee and default column kind (TODO, IN_PROGRESS, DONE, HISTORY). Requires the 'kanban:read' permission.")
    @GetMapping("/work-items")
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<List<WorkItemResponse>>> workItems(
            @RequestParam(required = false) UUID boardId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String columnKind) {
        BoardColumn.ColumnKind kind = parseColumnKind(columnKind);
        return ResponseEntity.ok(ApiResponse.ok(boardService.listWorkItems(boardId, assigneeId, kind)));
    }

    @Operation(summary = "Supervisor work items", description = "Lists all cards across all tenants with advanced filters. Requires superAdmin.")
    @GetMapping("/supervisor-items")
    @PreAuthorize("hasAuthority('super:admin')")
    public ResponseEntity<ApiResponse<List<WorkItemResponse>>> supervisorItems(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID boardId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String columnKind,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo,
            @RequestParam(required = false) String label) {
        BoardColumn.ColumnKind kind = parseColumnKind(columnKind);
        Card.Priority prio = parsePriority(priority);
        return ResponseEntity.ok(ApiResponse.ok(boardService.listAllForSupervisor(
                tenantId, boardId, assigneeId, kind, prio, dueDateFrom, dueDateTo, label)));
    }

    private BoardColumn.ColumnKind parseColumnKind(String columnKind) {
        if (columnKind == null || columnKind.isBlank()) return null;
        try {
            return BoardColumn.ColumnKind.valueOf(columnKind.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ControlTowerException(
                    "Invalid columnKind. Use TODO, IN_PROGRESS, DONE, or HISTORY.", HttpStatus.BAD_REQUEST);
        }
    }

    private Card.Priority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) return null;
        try {
            return Card.Priority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ControlTowerException(
                    "Invalid priority. Use LOW, MEDIUM, HIGH, or CRITICAL.", HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Move card to column by kind", description = "Move a card to a column of the specified kind within the same board.")
    @PatchMapping("/cards/{cardId}/move-to")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<WorkItemResponse>> moveCardTo(
            @PathVariable UUID cardId,
            @RequestBody Map<String, String> request) {
        String boardId = request.get("boardId");
        String targetKind = request.get("columnKind");
        if (boardId == null || targetKind == null) {
            throw new ControlTowerException("boardId and columnKind are required", HttpStatus.BAD_REQUEST);
        }
        UUID boardUuid = UUID.fromString(boardId);
        BoardColumn.ColumnKind kind = parseColumnKind(targetKind);
        WorkItemResponse result = boardService.moveCardToBoardColumn(cardId, boardUuid, kind);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
