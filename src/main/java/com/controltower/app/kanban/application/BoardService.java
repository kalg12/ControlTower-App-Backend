package com.controltower.app.kanban.application;

import com.controltower.app.kanban.api.dto.*;
import com.controltower.app.kanban.domain.*;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.events.UserActionEvent;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository          boardRepository;
    private final BoardColumnRepository    columnRepository;
    private final CardRepository           cardRepository;
    private final ChecklistItemRepository  checklistRepository;
    private final ApplicationEventPublisher publisher;
    private final TenantRepository         tenantRepository;
    private final UserRepository          userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Boards ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BoardResponse> listBoards(Pageable pageable) {
        return boardRepository.findByTenantIdAndDeletedAtIsNull(
                TenantContext.getTenantId(), pageable)
                .map(this::toBoardSummary);
    }

    @Transactional
    public BoardResponse createBoard(BoardRequest request, UUID userId) {
        Board board = new Board();
        board.setTenantId(TenantContext.getTenantId());
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setVisibility(request.getVisibility());
        board.setCreatedBy(userId);
        board.setClientId(request.getClientId());
        board = boardRepository.save(board);
        seedDefaultColumns(board);
        UUID newBoardId = board.getId();
        entityManager.flush();
        entityManager.clear();
        Board loaded = resolveBoard(newBoardId);
        log.info("Created Kanban board {} with {} default columns", newBoardId, loaded.getBoardColumns().size());

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(userId)
                .actionName("BOARD_CREATED")
                .entityType("KanbanBoard")
                .entityId(newBoardId)
                .description("Created board '" + request.getName() + "'")
                .build());

        return toBoardDetail(loaded);
    }

    private void seedDefaultColumns(Board board) {
        record Seed(String name, BoardColumn.ColumnKind kind, int position) {}
        List<Seed> seeds = List.of(
                new Seed("To Do",       BoardColumn.ColumnKind.TODO,        0),
                new Seed("In Progress", BoardColumn.ColumnKind.IN_PROGRESS, 1),
                new Seed("Done",        BoardColumn.ColumnKind.DONE,        2),
                new Seed("History",     BoardColumn.ColumnKind.HISTORY,     3)
        );
        for (Seed s : seeds) {
            BoardColumn col = new BoardColumn();
            col.setBoard(board);
            col.setName(s.name());
            col.setColumnKind(s.kind());
            col.setPosition(s.position());
            columnRepository.save(col);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> listWorkItems(UUID boardId, UUID assigneeId, BoardColumn.ColumnKind columnKind) {
        UUID tenantId = TenantContext.getTenantId();
        List<Card> cards = cardRepository.findWorkItems(tenantId, boardId, assigneeId, columnKind);
        List<WorkItemResponse> out = new ArrayList<>(cards.size());
        for (Card c : cards) {
            BoardColumn col = c.getBoardColumn();
            Board b = col.getBoard();
            out.add(WorkItemResponse.builder()
                    .id(c.getId())
                    .card(toCardResponse(c))
                    .boardId(b.getId())
                    .boardName(b.getName())
                    .columnId(col.getId())
                    .columnName(col.getName())
                    .columnKind(col.getColumnKind() != null ? col.getColumnKind().name() : null)
                    .tenantId(b.getTenantId())
                    .tenantName(getTenantName(b.getTenantId()))
                    .assigneeNames(getAssigneeNames(c))
                    .checklistProgress(getChecklistProgress(c))
                    .overdue(isOverdue(c))
                    .build());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> listAllForSupervisor(
            UUID tenantId,
            UUID boardId,
            UUID assigneeId,
            BoardColumn.ColumnKind columnKind,
            Card.Priority priority,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            String label) {
        List<Card> cards = new ArrayList<>(cardRepository.findAllForSupervisorBase(
                tenantId, boardId, assigneeId, dueDateFrom, dueDateTo, label));
        if (columnKind != null) cards.removeIf(c -> columnKind != c.getBoardColumn().getColumnKind());
        if (priority != null)   cards.removeIf(c -> priority != c.getPriority());
        List<WorkItemResponse> out = new ArrayList<>(cards.size());
        for (Card c : cards) {
            BoardColumn col = c.getBoardColumn();
            Board b = col.getBoard();
            out.add(WorkItemResponse.builder()
                    .id(c.getId())
                    .card(toCardResponse(c))
                    .boardId(b.getId())
                    .boardName(b.getName())
                    .columnId(col.getId())
                    .columnName(col.getName())
                    .columnKind(col.getColumnKind() != null ? col.getColumnKind().name() : null)
                    .tenantId(b.getTenantId())
                    .tenantName(getTenantName(b.getTenantId()))
                    .assigneeNames(getAssigneeNames(c))
                    .checklistProgress(getChecklistProgress(c))
                    .overdue(isOverdue(c))
                    .build());
        }
        return out;
    }

    private String getTenantName(UUID tenantId) {
        if (tenantId == null) return null;
        return tenantRepository.findById(tenantId)
                .map(Tenant::getName)
                .orElse(null);
    }

private List<String> getAssigneeNames(Card card) {
        if (card.getAssigneeIds() == null || card.getAssigneeIds().isEmpty()) {
            return List.of();
        }
        return card.getAssigneeIds().stream()
                .map(id -> userRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(User::getFullName)
                .collect(java.util.stream.Collectors.toList());
    }

    private String getChecklistProgress(Card card) {
        if (card.getChecklist() == null || card.getChecklist().isEmpty()) {
            return null;
        }
        long completed = card.getChecklist().stream().filter(ChecklistItem::isCompleted).count();
        long total = card.getChecklist().size();
        return completed + "/" + total;
    }

    private boolean isOverdue(Card card) {
        if (card.getDueDate() == null) return false;
        return card.getDueDate().isBefore(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(UUID boardId) {
        Board board = resolveBoard(boardId);
        return toBoardDetail(board);
    }

    @Transactional
    public BoardResponse updateBoard(UUID boardId, BoardRequest request) {
        Board board = resolveBoard(boardId);
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setVisibility(request.getVisibility());
        return toBoardSummary(boardRepository.save(board));
    }

    @Transactional
    public void deleteBoard(UUID boardId) {
        Board board = resolveBoard(boardId);
        String boardName = board.getName();
        board.softDelete();
        boardRepository.save(board);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("BOARD_DELETED")
                .entityType("KanbanBoard")
                .entityId(boardId)
                .description("Deleted board '" + boardName + "'")
                .build());
    }

    // ── Columns ───────────────────────────────────────────────────────

    @Transactional
    public ColumnResponse addColumn(UUID boardId, String name, int position) {
        Board board = resolveBoard(boardId);
        BoardColumn column = new BoardColumn();
        column.setBoard(board);
        column.setName(name);
        column.setPosition(position);
        return toColumnResponse(columnRepository.save(column));
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", columnId));
        columnRepository.delete(column);
    }

    // ── Cards ─────────────────────────────────────────────────────────

    @Transactional
    public CardResponse createCard(CardRequest request) {
        BoardColumn column = columnRepository.findById(request.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", request.getColumnId()));
        Card card = new Card();
        card.setBoardColumn(column);
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        if (request.getAssigneeIds() != null) card.setAssigneeIds(request.getAssigneeIds());
        card.setDueDate(request.getDueDate());
        card.setPriority(request.getPriority());
        card.setPosition(request.getPosition());
        card.setEstimatedMinutes(request.getEstimatedMinutes());
        card.setClientId(request.getClientId());
        card = cardRepository.save(card);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("CARD_CREATED")
                .entityType("KanbanCard")
                .entityId(card.getId())
                .description("Created card '" + card.getTitle() + "' in '" + column.getName() + "'")
                .metadata(Map.of("boardId", column.getBoard().getId().toString(),
                                 "columnId", column.getId().toString()))
                .build());

        return toCardResponse(card);
    }

    @Transactional
    public CardResponse moveCard(UUID cardId, MoveCardRequest request) {
        Card card = resolveCard(cardId);
        String cardTitle = card.getTitle();
        String fromColumn = card.getBoardColumn().getName();
        UUID userId = resolveUserId();

        boolean wasOverdue = card.isOverdue() && card.getAttendedAt() == null;
        boolean movedAwayFromDone = card.getBoardColumn().getColumnKind() == BoardColumn.ColumnKind.DONE
                || card.getBoardColumn().getColumnKind() == BoardColumn.ColumnKind.HISTORY;

        BoardColumn target = columnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", request.getTargetColumnId()));
        boolean movedToDone = target.getColumnKind() == BoardColumn.ColumnKind.DONE
                || target.getColumnKind() == BoardColumn.ColumnKind.HISTORY;

        if (!movedAwayFromDone && !movedToDone && (wasOverdue || card.isOverdue())) {
            card.attend(userId, wasOverdue);
            awardPoints(userId);
            log.info("User {} attended overdue card {} - awarding 10 points", userId, cardId);
        } else if (movedAwayFromDone && !movedToDone) {
            card.attend(userId, card.isWasOverdue());
        }

        card.setBoardColumn(target);
        card.setPosition(request.getPosition());
        card = cardRepository.save(card);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(userId)
                .actionName("CARD_MOVED")
                .entityType("KanbanCard")
                .entityId(cardId)
                .description("Moved card '" + cardTitle + "' from '" + fromColumn + "' to '" + target.getName() + "'")
                .metadata(Map.of("fromColumn", fromColumn, "toColumn", target.getName(), "wasOverdue", String.valueOf(wasOverdue)))
                .build());

        return toCardResponse(card);
    }

    @Transactional
    public CardResponse updateCard(UUID cardId, CardUpdateRequest request) {
        Card card = resolveCard(cardId);
        boolean wasOverdue = card.isOverdue() && card.getAttendedAt() == null;
        
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        if (request.getAssigneeIds() != null) card.setAssigneeIds(request.getAssigneeIds());
        card.setDueDate(request.getDueDate());
        card.setPriority(request.getPriority());
        card.setEstimatedMinutes(request.getEstimatedMinutes());
        
        if (wasOverdue) {
            UUID userId = resolveUserId();
            card.attend(userId, true);
            awardPoints(userId);
            log.info("User {} attended overdue card {} via update - awarding 10 points", userId, cardId);
        }
        
        return toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse attendCard(UUID cardId) {
        Card card = resolveCard(cardId);
        if (card.getAttendedAt() != null) return toCardResponse(card);
        UUID userId = resolveUserId();
        card.attend(userId, card.isOverdue());
        awardPoints(userId);
        log.info("User {} manually attended card {}", userId, cardId);
        return toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Card card = resolveCard(cardId);
        String cardTitle = card.getTitle();
        card.softDelete();
        cardRepository.save(card);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("CARD_DELETED")
                .entityType("KanbanCard")
                .entityId(cardId)
                .description("Deleted card '" + cardTitle + "'")
                .build());
    }

    // ── Checklist ─────────────────────────────────────────────────────

    @Transactional
    public ChecklistItemResponse addChecklistItem(UUID cardId, String text) {
        Card card = resolveCard(cardId);
        ChecklistItem item = new ChecklistItem();
        item.setCard(card);
        item.setText(text);
        item.setPosition(card.getChecklist().size());
        return toChecklistItemResponse(checklistRepository.save(item));
    }

    @Transactional
    public ChecklistItemResponse toggleChecklistItem(UUID itemId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", itemId));
        item.setCompleted(!item.isCompleted());
        ChecklistItem saved = checklistRepository.save(item);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("CHECKLIST_TOGGLED")
                .entityType("ChecklistItem")
                .entityId(itemId)
                .description((saved.isCompleted() ? "Completed" : "Uncompleted") + " checklist item '" + saved.getText() + "'")
                .metadata(Map.of("cardId", saved.getCard().getId().toString(),
                                 "completed", saved.isCompleted()))
                .build());

        return toChecklistItemResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Board resolveBoard(UUID boardId) {
        return boardRepository
                .findByIdTenantAndFetchColumns(boardId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }

    private Card resolveCard(UUID cardId) {
        return cardRepository.findByIdAndDeletedAtIsNull(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));
    }

    /** Resolves the current authenticated user's UUID from the SecurityContext. */
    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try { return UUID.fromString(auth.getName()); }
            catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private BoardResponse toBoardSummary(Board b) {
        return BoardResponse.builder()
                .id(b.getId())
                .tenantId(b.getTenantId())
                .name(b.getName())
                .description(b.getDescription())
                .visibility(b.getVisibility().name())
                .createdBy(b.getCreatedBy())
                .clientId(b.getClientId())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .columns(null)
                .build();
    }

    private BoardResponse toBoardDetail(Board b) {
        List<ColumnResponse> cols = b.getBoardColumns().stream()
                .map(this::toColumnResponse)
                .toList();
        return BoardResponse.builder()
                .id(b.getId())
                .tenantId(b.getTenantId())
                .name(b.getName())
                .description(b.getDescription())
                .visibility(b.getVisibility().name())
                .createdBy(b.getCreatedBy())
                .clientId(b.getClientId())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .columns(cols)
                .build();
    }

    private ColumnResponse toColumnResponse(BoardColumn c) {
        List<CardResponse> cards = c.getCards().stream()
                .filter(card -> card.getDeletedAt() == null)
                .map(this::toCardResponse)
                .toList();
        return ColumnResponse.builder()
                .id(c.getId())
                .boardId(c.getBoard().getId())
                .name(c.getName())
                .columnKind(c.getColumnKind() != null ? c.getColumnKind().name() : null)
                .position(c.getPosition())
                .wipLimit(c.getWipLimit())
                .cards(cards)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private CardResponse toCardResponse(Card c) {
        List<ChecklistItemResponse> checklist = c.getChecklist().stream()
                .map(this::toChecklistItemResponse)
                .toList();
        return CardResponse.builder()
                .id(c.getId())
                .columnId(c.getBoardColumn().getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .assigneeIds(c.getAssigneeIds())
                .dueDate(c.getDueDate())
                .priority(c.getPriority().name())
                .position(c.getPosition())
                .labels(c.getLabels())
                .checklist(checklist)
                .estimatedMinutes(c.getEstimatedMinutes())
                .attendedBy(c.getAttendedBy())
                .attendedAt(c.getAttendedAt())
                .wasOverdue(c.isWasOverdue())
                .clientId(c.getClientId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ChecklistItemResponse toChecklistItemResponse(ChecklistItem i) {
        return ChecklistItemResponse.builder()
                .id(i.getId())
                .cardId(i.getCard().getId())
                .text(i.getText())
                .completed(i.isCompleted())
                .position(i.getPosition())
                .createdAt(i.getCreatedAt())
                .build();
    }

    @Transactional
    public WorkItemResponse moveCardToBoardColumn(UUID cardId, UUID boardId, BoardColumn.ColumnKind targetKind) {
        Card card = resolveCard(cardId);
        UUID userId = resolveUserId();

        List<BoardColumn> columns = columnRepository.findByBoardId(boardId);
        BoardColumn targetColumn = columns.stream()
                .filter(col -> col.getColumnKind() == targetKind)
                .findFirst()
                .orElseThrow(() -> new ControlTowerException(
                        "Column not found with kind: " + targetKind, HttpStatus.BAD_REQUEST));

        BoardColumn currentColumn = card.getBoardColumn();
        boolean wasOverdue = card.isOverdue() && card.getAttendedAt() == null;
        boolean movedAwayFromDone = currentColumn.getColumnKind() == BoardColumn.ColumnKind.DONE
                || currentColumn.getColumnKind() == BoardColumn.ColumnKind.HISTORY;
        boolean movedToDone = targetColumn.getColumnKind() == BoardColumn.ColumnKind.DONE
                || targetColumn.getColumnKind() == BoardColumn.ColumnKind.HISTORY;

        if (!movedAwayFromDone && !movedToDone && (wasOverdue || card.isOverdue())) {
            card.attend(userId, wasOverdue);
            awardPoints(userId);
            log.info("User {} attended overdue card {} - awarding 10 points", userId, cardId);
        } else if (movedAwayFromDone && !movedToDone) {
            card.attend(userId, card.isWasOverdue());
        }

        card.setBoardColumn(targetColumn);
        card.setPosition(targetColumn.getCards().size());
        card = cardRepository.save(card);

        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(userId)
                .actionName("CARD_MOVED_VIA_WORKHUB")
                .entityType("KanbanCard")
                .entityId(cardId)
                .description("Moved card '" + card.getTitle() + "' to " + targetKind.name())
                .build());

        BoardColumn col = card.getBoardColumn();
        Board b = col.getBoard();
        return WorkItemResponse.builder()
                .id(card.getId())
                .card(toCardResponse(card))
                .boardId(b.getId())
                .boardName(b.getName())
                .columnId(col.getId())
                .columnName(col.getName())
                .columnKind(col.getColumnKind() != null ? col.getColumnKind().name() : null)
                .tenantId(b.getTenantId())
                .tenantName(getTenantName(b.getTenantId()))
                .assigneeNames(getAssigneeNames(card))
                .checklistProgress(getChecklistProgress(card))
                .overdue(isOverdue(card))
                .build();
    }

    private void awardPoints(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.addKanbanPoints(10);
            user.incrementOverdueAttended();
            userRepository.save(user);
            log.info("User {} earned 10 points for attending overdue card (total: {})", user.getFullName(), user.getKanbanPoints());
        });
    }
}
