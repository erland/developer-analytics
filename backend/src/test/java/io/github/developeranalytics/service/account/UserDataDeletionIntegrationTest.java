package io.github.developeranalytics.service.account;

import io.github.developeranalytics.auth.CryptoTokens;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("persistence")
@Tag("privacy")
class UserDataDeletionIntegrationTest {

    @Inject EntityManager entityManager;
    @Inject UserDataDeletionService deletionService;

    @Test
    @Transactional
    void deletesRealisticRelationalGraphThroughDatabaseCascades() {
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();
        UUID backgroundJobId = UUID.randomUUID();
        UUID repositorySyncRunId = UUID.randomUUID();
        UUID userMonthId = UUID.randomUUID();
        UUID repositoryMonthId = UUID.randomUUID();
        UUID technologyMonthId = UUID.randomUUID();

        entityManager.createNativeQuery(
                "insert into app_user(id) values (:id)"
        ).setParameter("id", userId).executeUpdate();

        entityManager.createNativeQuery(
                "insert into provider_identity(" +
                "id,user_id,provider,external_user_id,login) " +
                "values(:id,:userId,'github',:externalId,'test-user')"
        )
        .setParameter("id", identityId)
        .setParameter("userId", userId)
        .setParameter("externalId", "ext-" + userId)
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into provider_connection(" +
                "id,user_id,provider_identity_id,provider,status) " +
                "values(:id,:userId,:identityId,'github','CONNECTED')"
        )
        .setParameter("id", connectionId)
        .setParameter("userId", userId)
        .setParameter("identityId", identityId)
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into user_session(" +
                "id,user_id,token_hash,expires_at,last_seen_at) " +
                "values(:id,:userId,:hash,:expiresAt,:now)"
        )
        .setParameter("id", sessionId)
        .setParameter("userId", userId)
        .setParameter("hash", CryptoTokens.sha256("session-token-" + userId))
        .setParameter("expiresAt", OffsetDateTime.now().plusHours(1))
        .setParameter("now", OffsetDateTime.now())
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into source_repository(" +
                "id,user_id,provider,external_repository_id,owner_login," +
                "owner_type,ownership_relation,name,visibility,is_fork," +
                "is_archived,sync_status,included_in_analysis) " +
                "values(:id,:userId,'github',:externalId,'owner','USER'," +
                "'OWNED_BY_USER','private-project','PRIVATE',false,false," +
                "'SYNCED',true)"
        )
        .setParameter("id", repositoryId)
        .setParameter("userId", userId)
        .setParameter("externalId", "repo-" + userId)
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into contribution(" +
                "id,user_id,source_repository_id,provider," +
                "provider_contribution_id,contribution_type,occurred_at) " +
                "values(:id,:userId,:repositoryId,'github',:externalId," +
                "'COMMIT',:occurredAt)"
        )
        .setParameter("id", contributionId)
        .setParameter("userId", userId)
        .setParameter("repositoryId", repositoryId)
        .setParameter("externalId", "commit-" + userId)
        .setParameter("occurredAt", OffsetDateTime.now())
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into background_job(" +
                "id,user_id,job_type,status,next_execution_at) " +
                "values(:id,:userId,'github-repository-discovery'," +
                "'QUEUED',:now)"
        )
        .setParameter("id", backgroundJobId)
        .setParameter("userId", userId)
        .setParameter("now", OffsetDateTime.now())
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into repository_sync_run(" +
                "id,user_id,provider,status) " +
                "values(:id,:userId,'github','COMPLETED')"
        )
        .setParameter("id", repositorySyncRunId)
        .setParameter("userId", userId)
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into user_activity_month(" +
                "id,user_id,year_month) values(:id,:userId,:month)"
        )
        .setParameter("id", userMonthId)
        .setParameter("userId", userId)
        .setParameter("month", java.time.LocalDate.of(2026, 8, 1))
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into repository_activity_month(" +
                "id,user_id,source_repository_id,year_month) " +
                "values(:id,:userId,:repositoryId,:month)"
        )
        .setParameter("id", repositoryMonthId)
        .setParameter("userId", userId)
        .setParameter("repositoryId", repositoryId)
        .setParameter("month", java.time.LocalDate.of(2026, 8, 1))
        .executeUpdate();

        entityManager.createNativeQuery(
                "insert into technology_activity_month(" +
                "id,user_id,technology_key,year_month) " +
                "values(:id,:userId,'java',:month)"
        )
        .setParameter("id", technologyMonthId)
        .setParameter("userId", userId)
        .setParameter("month", java.time.LocalDate.of(2026, 8, 1))
        .executeUpdate();

        entityManager.flush();

        var result = deletionService.deleteUser(userId);

        assertEquals(1L, result.deletedDataCounts().get("providerConnections"));
        assertEquals(1L, result.deletedDataCounts().get("repositories"));
        assertEquals(1L, result.deletedDataCounts().get("contributions"));
        assertEquals(1L, result.deletedDataCounts().get("backgroundJobs"));

        for (String table : new String[] {
                "app_user",
                "provider_identity",
                "provider_connection",
                "user_session",
                "source_repository",
                "contribution",
                "background_job",
                "repository_sync_run",
                "user_activity_month",
                "repository_activity_month",
                "technology_activity_month"
        }) {
            Number count = (Number) entityManager.createNativeQuery(
                    "select count(*) from " + table +
                    " where " + userColumn(table) + "=:userId"
            )
            .setParameter("userId", userId)
            .getSingleResult();

            assertEquals(0L, count.longValue(), table);
        }
    }

    private String userColumn(String table) {
        return table.equals("app_user") ? "id" : "user_id";
    }
}
