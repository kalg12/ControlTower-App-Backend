package com.controltower.app.proposals.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID>,
                                             JpaSpecificationExecutor<Proposal> {

    Optional<Proposal> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Proposal> findByEmailTrackingToken(UUID emailTrackingToken);

    Optional<Proposal> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("""
        SELECT p FROM Proposal p
        WHERE p.tenantId = :tenantId
          AND p.clientId = :clientId
          AND p.deletedAt IS NULL
          AND (:status IS NULL OR p.status = :status)
        """)
    Page<Proposal> findByClientId(
            @Param("tenantId") UUID tenantId,
            @Param("clientId") UUID clientId,
            @Param("status") ProposalStatus status,
            Pageable pageable);

    @Query("""
        SELECT COUNT(p) FROM Proposal p
        WHERE p.tenantId = :tenantId
          AND EXTRACT(YEAR FROM p.createdAt) = :year
          AND p.deletedAt IS NULL
        """)
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);

    @Query("""
        SELECT p FROM Proposal p
        WHERE p.deletedAt IS NULL
          AND p.validityDate < :today
          AND p.status IN (com.controltower.app.proposals.domain.ProposalStatus.SENT,
                           com.controltower.app.proposals.domain.ProposalStatus.VIEWED)
        """)
    List<Proposal> findExpiredCandidates(@Param("today") LocalDate today);

    @Modifying
    @Query("""
        UPDATE Proposal p SET p.status = com.controltower.app.proposals.domain.ProposalStatus.EXPIRED
        WHERE p.deletedAt IS NULL
          AND p.validityDate < :today
          AND p.status IN (com.controltower.app.proposals.domain.ProposalStatus.SENT,
                           com.controltower.app.proposals.domain.ProposalStatus.VIEWED)
        """)
    int bulkExpire(@Param("today") LocalDate today);
}
