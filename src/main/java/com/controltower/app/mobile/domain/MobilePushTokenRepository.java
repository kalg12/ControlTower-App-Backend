package com.controltower.app.mobile.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobilePushTokenRepository extends JpaRepository<MobilePushToken, UUID> {

    List<MobilePushToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<MobilePushToken> findByUserIdAndToken(UUID userId, String token);

    @Modifying
    @Query("UPDATE MobilePushToken t SET t.active = false WHERE t.token = :token")
    void deactivateByToken(@Param("token") String token);
}
