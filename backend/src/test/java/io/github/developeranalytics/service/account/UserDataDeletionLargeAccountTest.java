package io.github.developeranalytics.service.account;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

@QuarkusTest
@Tag("persistence")
@Tag("privacy")
class UserDataDeletionLargeAccountTest {

    private static final int REPOSITORY_COUNT = 200;
    private static final int CONTRIBUTIONS_PER_REPOSITORY = 50;
    private static final int FILE_CHANGES_PER_CONTRIBUTION = 4;

    @Inject EntityManager entityManager;
    @Inject UserDataDeletionService deletionService;

    @Test
    void deletesLargeAccountWithinOperationalTimeoutBudget() {
        UUID userId = UUID.randomUUID();

        QuarkusTransaction.requiringNew().run(() -> seedLargeAccount(userId));

        var result = assertTimeout(
                Duration.ofSeconds(20),
                () -> deletionService.deleteUser(userId)
        );

        assertEquals((long) REPOSITORY_COUNT, result.deletedDataCounts().get("repositories"));
        assertEquals(
                (long) REPOSITORY_COUNT * CONTRIBUTIONS_PER_REPOSITORY,
                result.deletedDataCounts().get("contributions")
        );

        Number remaining = (Number) entityManager.createNativeQuery(
                "select count(*) from app_user where id=:userId"
        ).setParameter("userId", userId).getSingleResult();
        assertEquals(0L, remaining.longValue());
    }

    private void seedLargeAccount(UUID userId) {
        entityManager.createNativeQuery(
                "insert into app_user(id) values (:userId)"
        ).setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("""
                insert into source_repository(
                    id,user_id,provider,external_repository_id,owner_login,
                    owner_type,ownership_relation,name,visibility,is_fork,
                    is_archived,sync_status,included_in_analysis
                )
                select
                    md5(cast(:userId as text) || '-repo-' || r)::uuid,
                    :userId,
                    'github',
                    'large-repo-' || r,
                    'large-account',
                    'USER',
                    'OWNED_BY_USER',
                    'large-repo-' || r,
                    'PUBLIC',
                    false,
                    false,
                    'SYNCED',
                    true
                from generate_series(1, :repositoryCount) r
                """)
                .setParameter("userId", userId)
                .setParameter("repositoryCount", REPOSITORY_COUNT)
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into contribution(
                    id,user_id,source_repository_id,provider,
                    provider_contribution_id,contribution_type,occurred_at
                )
                select
                    md5(cast(:userId as text) || '-commit-' || r || '-' || c)::uuid,
                    :userId,
                    md5(cast(:userId as text) || '-repo-' || r)::uuid,
                    'github',
                    'large-commit-' || r || '-' || c,
                    'COMMIT',
                    current_timestamp
                from generate_series(1, :repositoryCount) r
                cross join generate_series(1, :contributionsPerRepository) c
                """)
                .setParameter("userId", userId)
                .setParameter("repositoryCount", REPOSITORY_COUNT)
                .setParameter("contributionsPerRepository", CONTRIBUTIONS_PER_REPOSITORY)
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into contribution_file_change(
                    id,contribution_id,user_id,source_repository_id,path,
                    additions,deletions,change_kind,classifier_confidence,
                    classifier_rule_key,classifier_version,occurred_at,
                    privacy_provenance
                )
                select
                    md5(cast(:userId as text) || '-file-' || r || '-' || c || '-' || f)::uuid,
                    md5(cast(:userId as text) || '-commit-' || r || '-' || c)::uuid,
                    :userId,
                    md5(cast(:userId as text) || '-repo-' || r)::uuid,
                    'src/file-' || f || '.java',
                    3,
                    1,
                    'PRODUCTION',
                    1.0,
                    'large-account-test',
                    'test-v1',
                    current_timestamp,
                    'PROVIDER_METADATA'
                from generate_series(1, :repositoryCount) r
                cross join generate_series(1, :contributionsPerRepository) c
                cross join generate_series(1, :fileChangesPerContribution) f
                """)
                .setParameter("userId", userId)
                .setParameter("repositoryCount", REPOSITORY_COUNT)
                .setParameter("contributionsPerRepository", CONTRIBUTIONS_PER_REPOSITORY)
                .setParameter("fileChangesPerContribution", FILE_CHANGES_PER_CONTRIBUTION)
                .executeUpdate();
    }
}
