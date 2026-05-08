package com.controltower.app.monitoring.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface RemoteLogRepository extends JpaRepository<RemoteLog, UUID> {

    @Query("""
            SELECT r FROM RemoteLog r
            WHERE r.tenantId = :tenantId
              AND (:level     IS NULL OR r.level        = :level)
              AND (:service   IS NULL OR LOWER(r.serviceName)   LIKE LOWER(CONCAT('%', :service, '%')))
              AND (:business  IS NULL OR LOWER(r.businessName)  LIKE LOWER(CONCAT('%', :business, '%')))
              AND (:from      IS NULL OR r.receivedAt  >= :from)
              AND (:to        IS NULL OR r.receivedAt  <= :to)
            ORDER BY r.receivedAt DESC
            """)
    Page<RemoteLog> findFiltered(
            @Param("tenantId")  UUID tenantId,
            @Param("level")     RemoteLog.Level level,
            @Param("service")   String service,
            @Param("business")  String business,
            @Param("from")      Instant from,
            @Param("to")        Instant to,
            Pageable pageable);
}
