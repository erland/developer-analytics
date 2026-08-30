package io.github.developeranalytics.auth;

import io.github.developeranalytics.domain.auth.*;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.github.developeranalytics.persistence.auth.AuthenticationRepository;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;
import java.time.*;
import java.util.Optional;

@ApplicationScoped
public class AuthenticationService {
    public static final String SESSION_COOKIE="developer_analytics_session";

    @Inject AuthenticationRepository repo;
    @Inject GitHubOAuthClient github;
    @Inject ProviderCredentialService credentials;
    @ConfigProperty(name="developer-analytics.session.hours", defaultValue="8") long sessionHours;
    @ConfigProperty(name="developer-analytics.session.cookie-secure", defaultValue="true") boolean secureCookie;

    @Transactional
    public URI startGitHubLogin(){
        String state=CryptoTokens.randomUrlToken(32);
        String verifier=CryptoTokens.randomUrlToken(48);
        repo.saveLoginAttempt(new AuthLoginAttempt(CryptoTokens.sha256(state),verifier,OffsetDateTime.now().plusMinutes(10)));
        return github.authorizationUri(state,CryptoTokens.sha256(verifier));
    }

    @Transactional
    public LoginResult finishGitHubLogin(String code,String state) throws Exception {
        AuthLoginAttempt attempt=repo.consumeLoginAttempt(CryptoTokens.sha256(state),OffsetDateTime.now())
            .orElseThrow(() -> new SecurityException("Invalid or expired login state"));
        String accessToken=github.exchangeCode(code,attempt.getPkceVerifier());
        GitHubUserProfile gh=github.currentUser(accessToken);

        ProviderIdentity identity=repo.findGitHubIdentity(Long.toString(gh.id()))
            .orElseGet(() -> repo.createIdentity(Long.toString(gh.id()),gh.login(),gh.name()));
        identity.updateProfile(gh.login(),gh.name());

        // The GitHub OAuth token is encrypted immediately and is never stored in plaintext.
        credentials.storeAccessToken(identity.getUser().getId(), "github", accessToken);

        String token=CryptoTokens.randomUrlToken(48);
        OffsetDateTime expiry=OffsetDateTime.now().plusHours(sessionHours);
        repo.createSession(identity.getUser(),CryptoTokens.sha256(token),expiry);
        return new LoginResult(token,expiry);
    }

    @Transactional
    public Optional<ProviderIdentity> currentIdentity(String rawToken){
        if(rawToken==null || rawToken.isBlank()) return Optional.empty();
        return repo.findValidSession(CryptoTokens.sha256(rawToken),OffsetDateTime.now())
            .map(s -> repo.findGitHubIdentityForUser(s.getUser().getId()).orElse(null));
    }

    @Transactional
    public void logout(String rawToken){ if(rawToken!=null && !rawToken.isBlank()) repo.deleteSession(CryptoTokens.sha256(rawToken)); }

    public String sessionCookie(String token, OffsetDateTime expiresAt){
        long seconds=Math.max(0,Duration.between(OffsetDateTime.now(),expiresAt).getSeconds());
        return SESSION_COOKIE+"="+token+"; Path=/; Max-Age="+seconds+"; HttpOnly; SameSite=Lax"+(secureCookie?"; Secure":"");
    }

    public String clearSessionCookie(){return SESSION_COOKIE+"=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"+(secureCookie?"; Secure":"");}

    public record LoginResult(String token, OffsetDateTime expiresAt) {}
}
