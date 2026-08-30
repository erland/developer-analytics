package io.github.developeranalytics.auth;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
public final class CryptoTokens {
    private static final SecureRandom RANDOM=new SecureRandom();
    private CryptoTokens(){}
    public static String randomUrlToken(int bytes){ byte[] b=new byte[bytes]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public static String sha256(String value){ try{ return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
}
