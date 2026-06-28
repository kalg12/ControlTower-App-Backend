package com.controltower.app.notes.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    Page<Note> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    /** Root notes only (no replies). */
    Page<Note> findByTenantIdAndLinkedToAndLinkedIdAndParentIdIsNullAndDeletedAtIsNull(
            UUID tenantId, String linkedTo, UUID linkedId, Pageable pageable);

    /** All notes including replies (for backward compat). */
    Page<Note> findByTenantIdAndLinkedToAndLinkedIdAndDeletedAtIsNull(
            UUID tenantId, String linkedTo, UUID linkedId, Pageable pageable);

    /** Replies to a parent note. */
    List<Note> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID parentId);

    Optional<Note> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
