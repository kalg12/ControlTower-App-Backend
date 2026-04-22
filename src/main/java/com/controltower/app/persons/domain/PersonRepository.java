package com.controltower.app.persons.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    Page<Person> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Person> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("""
        SELECT p FROM Person p
        WHERE p.tenantId = :tenantId
          AND p.deletedAt IS NULL
          AND (:search IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
                               OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%',:search,'%'))
                               OR LOWER(p.email)     LIKE LOWER(CONCAT('%',:search,'%'))
                               OR LOWER(p.phone)     LIKE LOWER(CONCAT('%',:search,'%')))
        ORDER BY p.firstName ASC, p.lastName ASC
        """)
    Page<Person> search(
        @Param("tenantId") UUID tenantId,
        @Param("search")   String search,
        Pageable pageable
    );

    List<Person> findByTenantIdAndClientIdAndDeletedAtIsNull(UUID tenantId, UUID clientId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Person.PersonStatus status);
}
