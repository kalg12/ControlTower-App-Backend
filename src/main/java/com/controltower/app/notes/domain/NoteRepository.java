package com.controltower.app.notes.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    Page<Note> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Note> findByTenantIdAndLinkedToAndLinkedIdAndDeletedAtIsNull(
            UUID tenantId, String linkedTo, UUID linkedId, Pageable pageable);

    Optional<Note> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
