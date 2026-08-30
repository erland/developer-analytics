package io.github.developeranalytics.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class CredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @ConfigProperty(name = "developer-analytics.credentials.encryption-key")
    String encodedKey;

    @ConfigProperty(name = "developer-analytics.credentials.key-version", defaultValue = "v1")
    String keyVersion;

    public EncryptedValue encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Credential must not be blank");
        }

        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String payload =
                    Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + "." +
                    Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);

            return new EncryptedValue(payload, keyVersion);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt provider credential", e);
        }
    }

    public String decrypt(String payload, String storedKeyVersion) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Encrypted credential must not be blank");
        }
        if (!keyVersion.equals(storedKeyVersion)) {
            throw new IllegalStateException(
                    "Credential key version " + storedKeyVersion +
                    " is not supported by active key version " + keyVersion);
        }

        try {
            String[] parts = payload.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted credential format");
            }

            byte[] nonce = Base64.getUrlDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt provider credential", e);
        }
    }

    private SecretKey key() {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Credential encryption key must be standard Base64 encoded", e);
        }

        if (raw.length != 32) {
            throw new IllegalStateException(
                    "Credential encryption key must decode to exactly 32 bytes");
        }

        return new SecretKeySpec(raw, "AES");
    }

    public record EncryptedValue(String ciphertext, String keyVersion) {
    }
}
