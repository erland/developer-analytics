package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.RepositoryOwnerType;
import io.github.developeranalytics.domain.model.RepositoryOwnershipRelation;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderUser;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.connection.ProviderSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class GitHubContributionSyncContextResolverTest {

    @Test
    void usesPersistedLoginWithoutCurrentUserRequest() throws Exception {
        FakeCredentials credentials = new FakeCredentials(new ProviderSession(
                new ProviderAccessToken("token"), "alice"));
        FakeGitHub github = new FakeGitHub();
        GitHubContributionSyncContextResolver resolver = resolver(credentials, github);

        SourceRepository repository = repository("alice", RepositoryVisibility.PRIVATE);
        var context = resolver.resolve(UUID.randomUUID(), repository);

        assertEquals("alice", context.userLogin());
        assertEquals("token", context.accessToken().value());
        assertFalse(github.currentUserRequested);
        assertEquals("alice/repo", context.providerRepository().fullName());
        assertEquals(io.github.developeranalytics.provider.ProviderRepository.Visibility.PRIVATE,
                context.providerRepository().visibility());
    }

    @Test
    void fallsBackToCurrentUserWhenPersistedLoginIsMissing() throws Exception {
        FakeCredentials credentials = new FakeCredentials(new ProviderSession(
                new ProviderAccessToken("token"), null));
        FakeGitHub github = new FakeGitHub();
        GitHubContributionSyncContextResolver resolver = resolver(credentials, github);

        SourceRepository repository = repository(null, RepositoryVisibility.PUBLIC);
        var context = resolver.resolve(UUID.randomUUID(), repository);

        assertEquals("remote-login", context.userLogin());
        assertTrue(github.currentUserRequested);
        assertEquals(io.github.developeranalytics.provider.ProviderRepository.OwnerType.OTHER,
                context.providerRepository().ownerType());
    }

    private GitHubContributionSyncContextResolver resolver(
            ProviderCredentialService credentials,
            GitHubProviderAdapter github
    ) {
        GitHubContributionSyncContextResolver resolver = new GitHubContributionSyncContextResolver();
        resolver.credentials = credentials;
        resolver.github = github;
        return resolver;
    }

    private SourceRepository repository(String ownerLogin, RepositoryVisibility visibility) {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-id", ownerLogin, "repo");
        repository.updateFromDiscovery(
                "owner-id",
                ownerLogin,
                "repo",
                ownerLogin == null ? "org/repo" : ownerLogin + "/repo",
                "https://github.com/example/repo",
                null,
                List.of(),
                ownerLogin == null ? RepositoryOwnerType.ORGANIZATION : RepositoryOwnerType.USER,
                ownerLogin == null
                        ? RepositoryOwnershipRelation.ORGANIZATION_MEMBERSHIP
                        : RepositoryOwnershipRelation.OWNED_BY_USER,
                visibility,
                false,
                false,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        return repository;
    }

    static final class FakeCredentials extends ProviderCredentialService {
        private final ProviderSession session;

        FakeCredentials(ProviderSession session) {
            this.session = session;
        }

        @Override
        public ProviderSession requireSession(UUID userId, String provider) {
            return session;
        }
    }

    static final class FakeGitHub extends GitHubProviderAdapter {
        boolean currentUserRequested;

        @Override
        public ProviderUser fetchCurrentUser(ProviderAccessToken accessToken) throws ProviderException {
            currentUserRequested = true;
            return new ProviderUser("1", "remote-login", "Remote User");
        }
    }
}
