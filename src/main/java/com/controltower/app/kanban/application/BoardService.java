package com.controltower.app.kanban.application;

import com.controltower.app.kanban.api.dto.BoardRequest;
import com.controltower.app.kanban.api.dto.CardRequest;
import com.controltower.app.kanban.api.dto.MoveCardRequest;
import com.controltower.app.kanban.domain.*;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<Board> listBoards(Pageable pageable) {
        return boardRepository.findByTenantIdAndDeletedAtIsNull(
                TenantContext.getTenantId(), pageable);
    }

    @Transactional
    public Board createBoard(BoardRequest request, UUID userId) {
        Board board = new Board();
        board.setTenantId(TenantContext.getTenantId());
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setVisibility(request.getVisibility());
        board.setCreatedBy(userId);
        return boardRepository.save(board);
    }

    @Transactional(readOnly = true)
    public Board getBoard(UUID boardId) {
        return resolveBoard(boardId);
    }

    @Transactional
    public Board updateBoard(UUID boardId, BoardRequest request) {
        Board board = resolveBoard(boardId);
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setVisibility(request.getVisibility());
        return boardRepository.save(board);
    }

    @Transactional
    public void deleteBoard(UUID boardId) {
        Board board = resolveBoard(boardId);
        board.softDelete();
        boardRepository.save(board);
    }

    // ── Columns ───────────────────────────────────────────────────────

    @Transactional
    public BoardColumn addColumn(UUID boardId, String name, int position) {
        Board board = resolveBoard(boardId);
        BoardColumn column = new BoardColumn();
        column.setBoard(board);
        column.setName(name);
        column.setPosition(position);
        return columnRepository.save(column);
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", columnId));
        columnRepository.delete(column);
    }

    // ── Cards ─────────────────────────────────────────────────────────

    @Transactional
    public Card createCard(CardRequest request) {
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
        return cardRepository.save(card);
    }

    @Transactional
    public Card moveCard(UUID cardId, MoveCardRequest request) {
        Card card = resolveCard(cardId);
        BoardColumn target = columnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", request.getTargetColumnId()));
        card.setColumn(target);
        card.setPosition(request.getPosition());
        return cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Card card = resolveCard(cardId);
        card.softDelete();
        cardRepository.save(card);
    }

    // ── Checklist ─────────────────────────────────────────────────────

    @Transactional
    public ChecklistItem addChecklistItem(UUID cardId, String text) {
        Card card = resolveCard(cardId);
        ChecklistItem item = new ChecklistItem();
        item.setCard(card);
        item.setText(text);
        item.setPosition(card.getChecklist().size());
        return checklistRepository.save(item);
    }

    @Transactional
    public ChecklistItem toggleChecklistItem(UUID itemId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", itemId));
        item.setCompleted(!item.isCompleted());
        return checklistRepository.save(item);
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
}
