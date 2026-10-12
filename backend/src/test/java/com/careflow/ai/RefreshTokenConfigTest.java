package com.careflow.ai;

import com.careflow.ai.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev")
public class RefreshTokenConfigTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void testRefreshExpirationPropertyIsBound() {
        Long expirationMs = (Long) ReflectionTestUtils.getField(refreshTokenService, "refreshExpirationMs");
        assertEquals(604800000L, expirationMs, "careflow.jwt.refresh-expiration-ms must be injected into RefreshTokenService");
    }
}

