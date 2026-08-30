package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.domain.auth.UserSession;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class MeScopingTest {

    @Inject
    EntityManager entityManager;

    @Test
    void unauthenticatedMeIsRejected() {
        given()
        .when()
            .get("/api/me")
        .then()
            .statusCode(401);
    }

    @Test
    @Transactional
    void repositoryEndpointsOnlyReturnCurrentUsersData() {
        AppUser userA = AppUser.create();
        entityManager.persist(userA);

        ProviderIdentity identityA = new ProviderIdentity(
                userA, "github", "1001", "alice", "Alice");
        entityManager.persist(identityA);

        AppUser userB = AppUser.create();
        entityManager.persist(userB);

        ProviderIdentity identityB = new ProviderIdentity(
                userB, "github", "1002", "bob", "Bob");
        entityManager.persist(identityB);

        SourceRepository own = new SourceRepository(
                userA, "github", "repo-a", "alice", "alice-repo");
        entityManager.persist(own);

        SourceRepository other = new SourceRepository(
                userB, "github", "repo-b", "bob", "bob-repo");
        entityManager.persist(other);

        String rawToken = "session-token-user-a";
        UserSession session = new UserSession(
                userA,
                CryptoTokens.sha256(rawToken),
                OffsetDateTime.now().plusHours(1));
        entityManager.persist(session);

        entityManager.flush();

        Cookie sessionCookie = new Cookie.Builder(
                AuthenticationService.SESSION_COOKIE, rawToken)
                .build();

        given()
            .cookie(sessionCookie)
        .when()
            .get("/api/me/repositories")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].name", equalTo("alice-repo"));

        given()
            .cookie(sessionCookie)
        .when()
            .get("/api/me/repositories/" + other.getId())
        .then()
            .statusCode(404);
    }
}
