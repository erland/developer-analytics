package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.support.TestFixtureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class MeScopingTest {
    @Inject
    TestFixtureService fixtures;

@Test
    void unauthenticatedMeIsRejected() {
        given()
        .when()
            .get("/api/me")
        .then()
            .statusCode(401);
    }


@Test
void repositoryEndpointsOnlyReturnCurrentUsersData() {
    AppUser userA = fixtures.createUserWithSession(
            "1001", "alice", "Alice", "session-token-user-a");
    SourceRepository repositoryA = fixtures.createRepository(
            userA, "repo-a", "alice", "repository-a");

    AppUser userB = fixtures.createUserWithSession(
            "1002", "bob", "Bob", "session-token-user-b");
    SourceRepository repositoryB = fixtures.createRepository(
            userB, "repo-b", "bob", "repository-b");

    Cookie cookie = new Cookie.Builder(
            AuthenticationService.SESSION_COOKIE,
            "session-token-user-a"
    ).build();

    given()
        .cookie(cookie)
    .when()
        .get("/api/me/repositories")
    .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].id", equalTo(repositoryA.getId().toString()));

    given()
        .cookie(cookie)
    .when()
        .get("/api/me/repositories/" + repositoryB.getId())
    .then()
        .statusCode(404);
}
}
