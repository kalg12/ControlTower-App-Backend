package com.controltower.app.kanban.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByIdAndDeletedAtIsNull(UUID id);

@Query("""
        SELECT c FROM Card c
        LEFT JOIN FETCH c.boardColumn col
        LEFT JOIN FETCH col.board b
        WHERE c.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND b.tenantId = :tenantId
          AND (:boardId IS NULL OR b.id = :boardId)
          AND (:assigneeId IS NULL OR c.assigneeIds IS NOT NULL AND EXISTS (SELECT a FROM c.assigneeIds a WHERE a = :assigneeId))
          AND (:columnKind IS NULL OR col.columnKind = :columnKind)
        ORDER BY b.name ASC, col.position ASC, c.position ASC
        """)
    List<Card> findWorkItems(
            @Param("tenantId") UUID tenantId,
            @Param("boardId") UUID boardId,
            @Param("assigneeId") UUID assigneeId,
            @Param("columnKind") BoardColumn.ColumnKind columnKind);

    @Query("""
        SELECT c FROM Card c
        LEFT JOIN FETCH c.boardColumn col
        LEFT JOIN FETCH col.board b
        WHERE c.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND (:tenantId IS NULL OR b.tenantId = :tenantId)
          AND (:boardId IS NULL OR b.id = :boardId)
          AND (:assigneeId IS NULL OR c.assigneeIds IS NOT NULL AND EXISTS (SELECT a FROM c.assigneeIds a WHERE a = :assigneeId))
          AND (:dueDateFrom IS NULL OR c.dueDate >= :dueDateFrom)
          AND (:dueDateTo IS NULL OR c.dueDate <= :dueDateTo)
          AND (:label IS NULL OR c.labels IS NOT NULL AND EXISTS (SELECT l FROM c.labels l WHERE l = :label))
        ORDER BY b.name ASC, col.position ASC, c.position ASC
        """)
    List<Card> findAllForSupervisorBase(
            @Param("tenantId") UUID tenantId,
            @Param("boardId") UUID boardId,
            @Param("assigneeId") UUID assigneeId,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("label") String label);

    @Query("""
        SELECT c FROM Card c
        JOIN c.boardColumn col
        JOIN col.board b
        WHERE c.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND c.dueDate = :dueDate
          AND SIZE(c.assigneeIds) > 0
        """)
    List<Card> findByDueDateAndHasAssignees(@Param("dueDate") LocalDate dueDate);

    @Query("""
        SELECT c FROM Card c
        JOIN c.boardColumn col
        JOIN col.board b
        WHERE c.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND c.dueDate < :today
          AND SIZE(c.assigneeIds) > 0
        """)
    List<Card> findOverdueWithAssignees(@Param("today") LocalDate today);

    @Query("""
        SELECT c FROM Card c
        JOIN c.boardColumn col
        WHERE c.deletedAt IS NULL
          AND c.estimatedMinutes IS NOT NULL
          AND c.estimatedMinutes > 0
          AND SIZE(c.assigneeIds) > 0
          AND col.columnKind NOT IN (
              com.controltower.app.kanban.domain.BoardColumn.ColumnKind.DONE,
              com.controltower.app.kanban.domain.BoardColumn.ColumnKind.HISTORY)
        """)
    List<Card> findActiveWithEstimates();
}
