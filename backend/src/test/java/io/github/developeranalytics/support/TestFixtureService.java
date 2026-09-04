package io.github.developeranalytics.support;

import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.domain.auth.UserSession;
import io.github.developeranalytics.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class TestFixtureService {

    @Inject
    EntityManager entityManager;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public AppUser createUserWithSession(
            String externalId,
            String login,
            String displayName,
            String rawSessionToken
    ) {
        AppUser user = AppUser.create();
        entityManager.persist(user);

        ProviderIdentity identity = new ProviderIdentity(
                user, "github", externalId, login, displayName);
        entityManager.persist(identity);

        entityManager.persist(new UserSession(
                user,
                CryptoTokens.sha256(rawSessionToken),
                OffsetDateTime.now().plusHours(1)
        ));

        entityManager.flush();
        return user;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public SourceRepository createRepository(
            AppUser user,
            String externalRepositoryId,
            String ownerLogin,
            String name
    ) {
        AppUser managedUser = entityManager.getReference(AppUser.class, user.getId());
        SourceRepository repository = new SourceRepository(
                managedUser,
                "github",
                externalRepositoryId,
                ownerLogin,
                name
        );
        entityManager.persist(repository);
        entityManager.flush();
        return repository;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Contribution createContribution(
            AppUser user,
            SourceRepository repository,
            String providerContributionId,
            Contribution.Type type,
            OffsetDateTime occurredAt
    ) {
        AppUser managedUser = entityManager.getReference(AppUser.class, user.getId());
        SourceRepository managedRepository = entityManager.getReference(SourceRepository.class, repository.getId());
        Contribution contribution = new Contribution(
                managedUser, managedRepository, "github", providerContributionId, type, occurredAt);
        entityManager.persist(contribution);
        entityManager.flush();
        return contribution;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void replaceActivityWeeks(
            AppUser user,
            SourceRepository repository,
            List<ActivityWeekFixture> weeks
    ) {
        entityManager.createNativeQuery(
                        "delete from repository_user_activity_week where user_id=:userId and repository_id=:repositoryId")
                .setParameter("userId", user.getId())
                .setParameter("repositoryId", repository.getId())
                .executeUpdate();
        OffsetDateTime observedAt = OffsetDateTime.now();
        for (ActivityWeekFixture week : weeks) {
            entityManager.createNativeQuery(
                            "insert into repository_user_activity_week " +
                                    "(user_id,repository_id,week_start,commits,additions,deletions,observed_at) " +
                                    "values (:userId,:repositoryId,:weekStart,:commits,:additions,:deletions,:observedAt)")
                    .setParameter("userId", user.getId())
                    .setParameter("repositoryId", repository.getId())
                    .setParameter("weekStart", week.weekStart())
                    .setParameter("commits", week.commits())
                    .setParameter("additions", week.additions())
                    .setParameter("deletions", week.deletions())
                    .setParameter("observedAt", observedAt)
                    .executeUpdate();
        }
        entityManager.flush();
    }

    public record ActivityWeekFixture(LocalDate weekStart, int commits, long additions, long deletions) {}

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ProviderConnection createGitHubConnection(
            AppUser user,
            String externalId,
            String login,
            String displayName
    ) {
        AppUser managedUser = entityManager.getReference(AppUser.class, user.getId());

        ProviderIdentity identity = entityManager.createQuery(
                "select i from ProviderIdentity i " +
                "where i.user.id = :userId and i.provider = :provider",
                ProviderIdentity.class)
            .setParameter("userId", user.getId())
            .setParameter("provider", "github")
            .getResultStream()
            .findFirst()
            .orElseGet(() -> {
                ProviderIdentity created = new ProviderIdentity(
                        managedUser, "github", externalId, login, displayName);
                entityManager.persist(created);
                return created;
            });

        ProviderConnection connection = new ProviderConnection(
                managedUser, identity, "github");
        entityManager.persist(connection);
        entityManager.flush();
        return connection;
    }
}
