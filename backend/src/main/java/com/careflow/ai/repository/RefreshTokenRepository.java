package com.careflow.ai.repository;

import com.careflow.ai.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

        @Modifying
    @Query("UPDATE RefreshToken t SET t.used = true WHERE t.id = :id AND t.used = false")
    int markUsed(@Param("id") UUID id);
}