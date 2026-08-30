package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.domain.auth.UserSession;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class MeSyncResourceTest {

    @Inject
    EntityManager entityManager;

    @Test
    void unauthenticatedSyncIsRejected() {
        given()
        .when()
            .post("/api/me/sync/github/repositories")
        .then()
            .statusCode(401);
    }

    @Test
    @Transactional
    void authenticatedUserCanQueueRepositoryDiscovery() {
        AppUser user = AppUser.create();
        entityManager.persist(user);

        ProviderIdentity identity = new ProviderIdentity(
                user, "github", "4001", "alice", "Alice");
        entityManager.persist(identity);

        String rawToken = "sync-session";
        entityManager.persist(new UserSession(
                user,
                CryptoTokens.sha256(rawToken),
                OffsetDateTime.now().plusHours(1)));

        entityManager.flush();

        Cookie cookie = new Cookie.Builder(
                AuthenticationService.SESSION_COOKIE, rawToken).build();

        given()
            .cookie(cookie)
        .when()
            .post("/api/me/sync/github/repositories")
        .then()
            .statusCode(202)
            .body("jobId", notNullValue())
            .body("jobType", equalTo("GITHUB_REPOSITORY_DISCOVERY"))
            .body("status", equalTo("QUEUED"));
    }
}
