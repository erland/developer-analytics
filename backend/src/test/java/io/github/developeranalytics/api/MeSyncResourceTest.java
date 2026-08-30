package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.support.TestFixtureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@Tag("authorization")
@Tag("worker-job")
@Tag("persistence")
class MeSyncResourceTest {
    @Inject
    TestFixtureService fixtures;

@Test
    void unauthenticatedSyncIsRejected() {
        given()
        .when()
            .post("/api/me/sync/github/repositories")
        .then()
            .statusCode(401);
    }


@Test
void authenticatedUserCanQueueRepositoryDiscovery() {
    fixtures.createUserWithSession(
            "4001", "alice", "Alice", "sync-session");

    Cookie cookie = new Cookie.Builder(
            AuthenticationService.SESSION_COOKIE,
            "sync-session"
    ).build();

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
