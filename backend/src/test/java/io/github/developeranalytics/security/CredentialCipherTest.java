package io.github.developeranalytics.security;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("authorization")
@Tag("privacy")
@Tag("unit")
class CredentialCipherTest {

    @Inject
    CredentialCipher cipher;

    @Test
    void encryptsAndDecryptsCredentialWithoutPlaintextPersistence() {
        String plaintext = "github-secret-token";

        CredentialCipher.EncryptedValue encrypted = cipher.encrypt(plaintext);

        assertNotEquals(plaintext, encrypted.ciphertext());
        assertFalse(encrypted.ciphertext().contains(plaintext));
        assertEquals("test-v1", encrypted.keyVersion());
        assertEquals(
                plaintext,
                cipher.decrypt(encrypted.ciphertext(), encrypted.keyVersion())
        );
    }

    @Test
    void encryptionUsesRandomNonce() {
        var one = cipher.encrypt("same-secret");
        var two = cipher.encrypt("same-secret");

        assertNotEquals(one.ciphertext(), two.ciphertext());
    }

    @Test
    void refusesCredentialEncryptedWithUnknownKeyVersion() {
        var encrypted = cipher.encrypt("secret");

        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(encrypted.ciphertext(), "old-key")
        );
    }
}
