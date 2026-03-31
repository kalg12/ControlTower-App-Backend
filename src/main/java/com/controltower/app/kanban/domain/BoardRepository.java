package com.controltower.app.kanban.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    Page<Board> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Board> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
