package com.controltower.app.kanban.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    Page<Board> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Board> findByTenantIdAndClientIdAndDeletedAtIsNull(UUID tenantId, UUID clientId, Pageable pageable);

    Optional<Board> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    /**
     * Loads the board and its columns in one query (needed after create + EM clear, and reliable for GET board).
     */
    @Query("""
            SELECT DISTINCT b FROM Board b
            LEFT JOIN FETCH b.boardColumns
            WHERE b.id = :id AND b.tenantId = :tenantId AND b.deletedAt IS NULL
            """)
    Optional<Board> findByIdTenantAndFetchColumns(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
