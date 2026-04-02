package com.controltower.app.kanban.application;

import com.controltower.app.kanban.api.dto.*;
import com.controltower.app.kanban.domain.*;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository        boardRepository;
    private final BoardColumnRepository  columnRepository;
    private final CardRepository         cardRepository;
    private final ChecklistItemRepository checklistRepository;

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
        return toBoardSummary(boardRepository.save(board));
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
        board.softDelete();
        boardRepository.save(board);
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
        card.setColumn(column);
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setAssigneeId(request.getAssigneeId());
        card.setDueDate(request.getDueDate());
        card.setPriority(request.getPriority());
        card.setPosition(request.getPosition());
        return toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse moveCard(UUID cardId, MoveCardRequest request) {
        Card card = resolveCard(cardId);
        BoardColumn target = columnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", request.getTargetColumnId()));
        card.setColumn(target);
        card.setPosition(request.getPosition());
        return toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Card card = resolveCard(cardId);
        card.softDelete();
        cardRepository.save(card);
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
        return toChecklistItemResponse(checklistRepository.save(item));
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Board resolveBoard(UUID boardId) {
        return boardRepository.findByIdAndTenantIdAndDeletedAtIsNull(boardId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }

    private Card resolveCard(UUID cardId) {
        return cardRepository.findByIdAndDeletedAtIsNull(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));
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
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .columns(null)
                .build();
    }

    private BoardResponse toBoardDetail(Board b) {
        List<ColumnResponse> cols = b.getColumns().stream()
                .map(this::toColumnResponse)
                .toList();
        return BoardResponse.builder()
                .id(b.getId())
                .tenantId(b.getTenantId())
                .name(b.getName())
                .description(b.getDescription())
                .visibility(b.getVisibility().name())
                .createdBy(b.getCreatedBy())
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
                .columnId(c.getColumn().getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .assigneeId(c.getAssigneeId())
                .dueDate(c.getDueDate())
                .priority(c.getPriority().name())
                .position(c.getPosition())
                .labels(c.getLabels())
                .checklist(checklist)
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
}
