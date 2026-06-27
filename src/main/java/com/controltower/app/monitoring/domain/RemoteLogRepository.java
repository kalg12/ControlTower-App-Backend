package com.controltower.app.monitoring.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RemoteLogRepository extends JpaRepository<RemoteLog, UUID>, JpaSpecificationExecutor<RemoteLog> {
}
