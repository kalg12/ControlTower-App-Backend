package com.controltower.app.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, UUID> {

    Optional<StripeCustomer> findByClientId(UUID clientId);

    Optional<StripeCustomer> findByStripeCustomerId(String stripeCustomerId);
}
