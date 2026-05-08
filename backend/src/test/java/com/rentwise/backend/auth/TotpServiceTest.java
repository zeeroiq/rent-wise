package com.rentwise.backend.auth;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TotpServiceTest {
    @Autowired
    private TotpService totpService;

    @Test
    void generatedCodeVerifiesAtSameInstant() {
        String secret = totpService.generateSecret();
        Instant instant = Instant.ofEpochSecond(1_700_000_000L);
        String code = totpService.codeAt(secret, instant);

        assertTrue(totpService.verifyAt(secret, code, instant));
        assertFalse(totpService.verifyAt(secret, "000000", instant));
    }
}
