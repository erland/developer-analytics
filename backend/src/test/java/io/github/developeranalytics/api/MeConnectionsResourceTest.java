package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.support.TestFixtureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@Tag("authorization")
@Tag("persistence")
class MeConnectionsResourceTest {
    @Inject
    TestFixtureService fixtures;

@Test
    void unauthenticatedConnectionsAreRejected() {
        given()
        .when()
            .get("/api/me/connections")
        .then()
            .statusCode(401);
    }


@Test
void onlyCurrentUsersConnectionsAreVisibleAndMutable() {
    AppUser userA = fixtures.createUserWithSession(
            "2001", "alice", "Alice", "connection-session-a");
    fixtures.createGitHubConnection(
            userA, "2101", "alice", "Alice");

    AppUser userB = fixtures.createUserWithSession(
            "2002", "bob", "Bob", "connection-session-b");
    fixtures.createGitHubConnection(
            userB, "2102", "bob", "Bob");

    Cookie cookie = new Cookie.Builder(
            AuthenticationService.SESSION_COOKIE,
            "connection-session-a"
    ).build();

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
        .contentType(ContentType.JSON)
        .body("{\"dataDisposition\":\"PRESERVE_ANALYSED_DATA\"}")
    .when()
        .post("/api/me/connections/github/disconnect")
    .then()
        .statusCode(200)
        .body("connection.status", equalTo("DISCONNECTED"))
        .body("dataDisposition", equalTo("PRESERVE_ANALYSED_DATA"))
        .body("analysedDataRemoved", equalTo(false));
}
}
