package com.controltower.app.audit.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification factory for building type-safe, dynamic audit log queries.
 * Avoids the PostgreSQL "cannot determine data type of parameter $N" error
 * that occurs with JPQL IS NULL checks on untyped bind parameters.
 */
public final class AuditLogSpecification {

    private AuditLogSpecification() {}

    public static Specification<AuditLog> filter(
            UUID tenantId,
            UUID userId,
            AuditAction action,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

