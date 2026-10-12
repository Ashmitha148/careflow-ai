package com.careflow.ai.service;

import com.careflow.ai.entity.RefreshToken;
import com.careflow.ai.entity.RefreshTokenFamily;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.RefreshTokenFamilyRepository;
import com.careflow.ai.repository.RefreshTokenRepository;
import com.careflow.ai.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages refresh-token rotation with reuse detection.
 *
 * On each refresh:
 *   1. Look up the refresh token by its SHA-256 hash.
 *   2. If the token is expired or its family is revoked → reject.
 *   3. If the token has ALREADY been used → revoke the entire family (theft
 *      signal) and reject.
 *   4. Otherwise mark the token used, issue a new refresh token in the same
 *      family, and return it.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository tokenRepository;
    private final RefreshTokenFamilyRepository familyRepository;
    private final JwtTokenProvider jwtTokenProvider;
    /**
     * REQUIRES_NEW template: reuse-detection revocations must survive the
     * rollback of the request transaction (see revokeFamilyImmediately).
     */
    private final TransactionTemplate requiresNewTx;

    @Value("${careflow.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository tokenRepository,
                               RefreshTokenFamilyRepository familyRepository,
                               JwtTokenProvider jwtTokenProvider,
                               PlatformTransactionManager transactionManager) {
        this.tokenRepository = tokenRepository;
        this.familyRepository = familyRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public String createRefreshToken(User user) {
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setUserId(user.getId());
        family.setFamilyHash(generateFamilyHash(user.getId()));
        family.setCreatedAt(Instant.now());
        familyRepository.save(family);
        return issueTokenInFamily(family);
    }

    @Transactional
    public String rotateRefreshToken(String presentedToken) {
        String hash = hashToken(presentedToken);

        RefreshToken existing = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RefreshTokenRevokedException("Unknown refresh token"));

        RefreshTokenFamily family = familyRepository.findById(existing.getFamilyId())
                .orElseThrow(() -> new RefreshTokenRevokedException("Token family not found"));

        if (family.isRevoked()) {
            throw new RefreshTokenRevokedException("Token family has been revoked");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh token expired");
        }

        if (existing.isUsed()) {
            log.warn("REFRESH TOKEN REUSE DETECTED — user {} family {} — revoking entire family",
                    family.getUserId(), family.getId());
            revokeFamilyImmediately(family.getId());
            throw new RefreshTokenReuseException(
                    "Refresh token reuse detected. All sessions for this user have been revoked.");
        }

        int updated = tokenRepository.markUsed(existing.getId());
        if (updated == 0) {
            revokeFamilyImmediately(family.getId());
            throw new RefreshTokenReuseException(
                    "Refresh token reuse detected. All sessions for this user have been revoked.");
        }

        return issueTokenInFamily(family);
    }

    /**
     * Persists a family revocation in its OWN transaction. The reuse-detection
     * path throws {@link RefreshTokenReuseException} right after calling this,
     * which rolls back the surrounding request transaction — without
     * REQUIRES_NEW the revocation would be rolled back with it and the whole
     * point of reuse detection (killing the family) would be silently lost.
     */
    private void revokeFamilyImmediately(UUID familyId) {
        requiresNewTx.executeWithoutResult(tx ->
                familyRepository.revokeFamily(familyId));
    }

    @Transactional
    public void revokeAllUserFamilies(UUID userId) {
        familyRepository.revokeAllUserFamilies(userId);
    }

    private String issueTokenInFamily(RefreshTokenFamily family) {
        String rawToken = jwtTokenProvider.generateRefreshToken(family.getUserId().toString());

        RefreshToken token = new RefreshToken();
        token.setFamilyId(family.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setCreatedAt(Instant.now());
        tokenRepository.save(token);

        return rawToken;
    }

    private String generateFamilyHash(UUID userId) {
        return hashToken(userId.toString() + System.nanoTime() + UUID.randomUUID());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}