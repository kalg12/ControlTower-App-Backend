package com.controltower.app.monitoring.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RemoteLogSpecification {

    private RemoteLogSpecification() {}

    public static Specification<RemoteLog> filter(
            UUID tenantId,
            RemoteLog.Level level,
            String service,
            String business,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Clear order-by for count queries to avoid join issues
            if (query != null && Long.class.equals(query.getResultType())) {
                query.orderBy(java.util.Collections.emptyList());
            }

            predicates.add(cb.equal(root.<UUID>get("tenantId"), tenantId));

            if (level != null) {
                predicates.add(cb.equal(root.<RemoteLog.Level>get("level"), level));
            }
            if (service != null && !service.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.<String>get("serviceName")),
                        "%" + service.toLowerCase() + "%"));
            }
            if (business != null && !business.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.<String>get("businessName")),
                        "%" + business.toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("receivedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("receivedAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
