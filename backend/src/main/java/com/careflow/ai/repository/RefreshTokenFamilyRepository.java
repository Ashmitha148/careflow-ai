package com.careflow.ai.repository;

import com.careflow.ai.entity.RefreshTokenFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, UUID> {

    @Modifying
    @Query("UPDATE RefreshTokenFamily f SET f.revoked = true WHERE f.id = :id AND f.revoked = false")
    int revokeFamily(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE RefreshTokenFamily f SET f.revoked = true WHERE f.userId = :userId AND f.revoked = false")
    int revokeAllUserFamilies(@Param("userId") UUID userId);
}