package com.controltower.app.clients.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientContactRepository extends JpaRepository<ClientContact, UUID> {

    List<ClientContact> findByClientIdOrderByPrimaryDescCreatedAtAsc(UUID clientId);

    Optional<ClientContact> findByIdAndClientId(UUID id, UUID clientId);
}
