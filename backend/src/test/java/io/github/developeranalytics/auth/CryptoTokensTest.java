package io.github.developeranalytics.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoTokensTest {
    @Test
    void createsRandomUrlSafeTokensAndStableHashes() {
        String a=CryptoTokens.randomUrlToken(32);
        String b=CryptoTokens.randomUrlToken(32);
        assertNotEquals(a,b);
        assertEquals(CryptoTokens.sha256("x"),CryptoTokens.sha256("x"));
        assertNotEquals(CryptoTokens.sha256("x"),CryptoTokens.sha256("y"));
    }
}
