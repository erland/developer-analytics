package io.github.developeranalytics.auth;

import io.github.developeranalytics.domain.auth.*;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.persistence.auth.AuthenticationRepository;
import io.github.developeranalytics.persistence.auth.ProviderConnectionRepository;
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
    @Inject ProviderConnectionRepository connections;
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
public URI startGitHubPrivateRepositoryAuthorisation(
        String rawSessionToken
) {
    ProviderIdentity identity = currentIdentity(rawSessionToken)
            .orElseThrow(() -> new SecurityException(
                    "Authenticated session required"
            ));

    String state = CryptoTokens.randomUrlToken(32);
    String verifier = CryptoTokens.randomUrlToken(48);

    repo.saveLoginAttempt(new AuthLoginAttempt(
            CryptoTokens.sha256(state),
            verifier,
            OffsetDateTime.now().plusMinutes(10),
            AuthLoginAttempt.Purpose.PRIVATE_REPOSITORY_ACCESS,
            identity.getUser()
    ));

    // This is the only flow that asks GitHub for the broad `repo`
    // scope. Normal sign-in intentionally requests no private-repo
    // permission.
    return github.authorizationUri(
            state,
            CryptoTokens.sha256(verifier),
            "repo"
    );
}

@Transactional
public void removeGitHubPrivateRepositoryAuthorisation(
        String rawSessionToken
) {
    ProviderIdentity current = currentIdentity(rawSessionToken)
            .orElseThrow(() -> new SecurityException(
                    "Authenticated session required"
            ));

    ProviderConnection connection = connections
            .findForUserAndProvider(
                    current.getUser().getId(),
                    "github"
            )
            .orElseThrow(() -> new IllegalStateException(
                    "GitHub connection is missing"
            ));

    // This disables private-repository use inside Developer Analytics.
    // GitHub OAuth permissions can also be revoked by the user in GitHub.
    connection.removePrivateRepositoryAccess();
}


@Transactional
public CallbackResult finishGitHubCallback(
        String code,
        String state,
        String rawSessionToken
) throws Exception {
    AuthLoginAttempt attempt = repo.consumeLoginAttempt(
            CryptoTokens.sha256(state),
            OffsetDateTime.now()
    ).orElseThrow(() -> new SecurityException(
            "Invalid or expired login state"
    ));

    String accessToken = github.exchangeCode(
            code,
            attempt.getPkceVerifier()
    );
    GitHubUserProfile gh = github.currentUser(accessToken);

    if (attempt.getPurpose() ==
            AuthLoginAttempt.Purpose.PRIVATE_REPOSITORY_ACCESS) {
        ProviderIdentity current = currentIdentity(rawSessionToken)
                .orElseThrow(() -> new SecurityException(
                        "Authenticated session required"
                ));

        if (attempt.getUser() == null ||
                !attempt.getUser().getId()
                        .equals(current.getUser().getId())) {
            throw new SecurityException(
                    "Authorisation belongs to another user"
            );
        }

        if (!Long.toString(gh.id())
                .equals(current.getExternalUserId())) {
            throw new SecurityException(
                    "GitHub account changed during authorisation"
            );
        }

        credentials.storeAccessToken(
                current.getUser().getId(),
                "github",
                accessToken
        );

        ProviderConnection connection = connections
                .findForUserAndProvider(
                        current.getUser().getId(),
                        "github"
                )
                .orElseThrow(() -> new IllegalStateException(
                        "GitHub connection is missing"
                ));
        connection.authorisePrivateRepositoryAccess();

        return CallbackResult.privateRepositoryAccess();
    }

    ProviderIdentity identity = repo.findGitHubIdentity(
            Long.toString(gh.id())
    ).orElseGet(() -> repo.createIdentity(
            Long.toString(gh.id()),
            gh.login(),
            gh.name()
    ));

    identity.updateProfile(gh.login(), gh.name());

    credentials.storeAccessToken(
            identity.getUser().getId(),
            "github",
            accessToken
    );

    String token = CryptoTokens.randomUrlToken(48);
    OffsetDateTime expiry =
            OffsetDateTime.now().plusHours(sessionHours);
    repo.createSession(
            identity.getUser(),
            CryptoTokens.sha256(token),
            expiry
    );

    return CallbackResult.login(token, expiry);
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

    public record CallbackResult(
            boolean privateRepositoryAccessAuthorised,
            String token,
            OffsetDateTime expiresAt
    ) {
        public static CallbackResult login(
                String token,
                OffsetDateTime expiresAt
        ) {
            return new CallbackResult(false, token, expiresAt);
        }

        public static CallbackResult privateRepositoryAccess() {
            return new CallbackResult(true, null, null);
        }
    }
}
