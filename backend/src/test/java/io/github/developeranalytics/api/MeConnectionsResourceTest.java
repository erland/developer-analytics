package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.domain.auth.UserSession;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderConnection;
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
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class MeConnectionsResourceTest {

    @Inject
    EntityManager entityManager;

    @Test
    void unauthenticatedConnectionsAreRejected() {
        given()
        .when()
            .get("/api/me/connections")
        .then()
            .statusCode(401);
    }

    @Test
    @Transactional
    void onlyCurrentUsersConnectionsAreVisibleAndMutable() {
        AppUser userA = AppUser.create();
        entityManager.persist(userA);

        ProviderIdentity identityA = new ProviderIdentity(
                userA, "github", "2001", "alice", "Alice");
        entityManager.persist(identityA);

        ProviderConnection connectionA = new ProviderConnection(
                userA, identityA, "github");
        entityManager.persist(connectionA);

        AppUser userB = AppUser.create();
        entityManager.persist(userB);

        ProviderIdentity identityB = new ProviderIdentity(
                userB, "github", "2002", "bob", "Bob");
        entityManager.persist(identityB);

        ProviderConnection connectionB = new ProviderConnection(
                userB, identityB, "github");
        entityManager.persist(connectionB);

        String rawToken = "connection-session-a";
        entityManager.persist(new UserSession(
                userA,
                CryptoTokens.sha256(rawToken),
                OffsetDateTime.now().plusHours(1)));

        entityManager.flush();

        Cookie cookie = new Cookie.Builder(
                AuthenticationService.SESSION_COOKIE, rawToken).build();

        given()
            .cookie(cookie)
        .when()
            .get("/api/me/connections")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].provider", equalTo("github"))
            .body("[0].login", equalTo("alice"))
            .body("[0].status", equalTo("CONNECTED"));

        given()
            .cookie(cookie)
        .when()
            .post("/api/me/connections/github/disconnect")
        .then()
            .statusCode(200)
            .body("status", equalTo("DISCONNECTED"));

        entityManager.flush();
        entityManager.refresh(connectionB);

        // Disconnecting user A's connection must not affect user B.
        org.junit.jupiter.api.Assertions.assertEquals(
                ProviderConnection.Status.CONNECTED.name(),
                connectionB.getStatus());
    }
}
