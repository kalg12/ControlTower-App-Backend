package com.controltower.app.kanban.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT c FROM Card c
        JOIN FETCH c.column col
        JOIN FETCH col.board b
        WHERE b.tenantId = :tenantId
          AND c.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND (:assigneeId IS NULL OR c.assigneeId = :assigneeId)
          AND (:columnKind IS NULL OR col.columnKind = :columnKind)
        ORDER BY b.name ASC, col.position ASC, c.position ASC
        """)
    List<Card> findWorkItems(
            @Param("tenantId") UUID tenantId,
            @Param("assigneeId") UUID assigneeId,
            @Param("columnKind") BoardColumn.ColumnKind columnKind);
}
