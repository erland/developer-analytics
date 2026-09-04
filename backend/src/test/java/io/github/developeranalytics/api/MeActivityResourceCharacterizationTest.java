package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.support.TestFixtureService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@Tag("persistence")
class MeActivityResourceCharacterizationTest {

    @Inject
    TestFixtureService fixtures;

    @Test
    void allTimeResponseCombinesCommitAndWeeklyLineStatistics() {
        AppUser user = fixtures.createUserWithSession(
                "activity-1001", "activity-alice", "Activity Alice", "activity-session-1001");
        SourceRepository alpha = fixtures.createRepository(
                user, "activity-repo-1001-a", "activity-alice", "alpha");
        SourceRepository beta = fixtures.createRepository(
                user, "activity-repo-1001-b", "activity-alice", "beta");

        fixtures.createContribution(user, alpha, "activity-c-1001-a1", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-01-05T10:00:00Z"));
        fixtures.createContribution(user, alpha, "activity-c-1001-a2", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-02-10T11:00:00Z"));
        fixtures.createContribution(user, beta, "activity-c-1001-b1", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-02-11T12:00:00Z"));
        fixtures.createContribution(user, beta, "activity-pr-1001-b1", Contribution.Type.PULL_REQUEST,
                OffsetDateTime.parse("2026-02-12T12:00:00Z"));

        fixtures.replaceActivityWeeks(user, alpha, List.of(
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-01-05"), 1, 10, 2),
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-02-09"), 1, 20, 3)));
        fixtures.replaceActivityWeeks(user, beta, List.of(
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-02-09"), 1, 7, 1)));

        Response response = given()
                .cookie(sessionCookie("activity-session-1001"))
        .when()
                .get("/api/me/activity");

        response.then()
                .statusCode(200)
                .body("commitCount", equalTo(3))
                .body("activeProjects", equalTo(2))
                .body("additions", equalTo(37))
                .body("deletions", equalTo(6))
                .body("lineStatisticsCommitCount", equalTo(3))
                .body("firstActivityAt", equalTo("2026-01-05T10:00:00Z"))
                .body("lastActivityAt", equalTo("2026-02-11T12:00:00Z"))
                .body("commitsPerYear", hasSize(1))
                .body("commitsPerYear[0].year", equalTo(2026))
                .body("commitsPerYear[0].commits", equalTo(3))
                .body("commitsPerYear[0].changedLines", equalTo(43))
                .body("commitsPerMonth", hasSize(2))
                .body("projectsOverTime", hasSize(2));

        Number averageCommitSize = response.jsonPath().get("averageCommitSize");
        assertThat(averageCommitSize.doubleValue(), closeTo(43.0 / 3.0, 0.0001));
    }

    @Test
    void monthFilterLimitsBothCommitAndWeeklyMetricsToResolvedPeriod() {
        AppUser user = fixtures.createUserWithSession(
                "activity-1002", "activity-bob", "Activity Bob", "activity-session-1002");
        SourceRepository repository = fixtures.createRepository(
                user, "activity-repo-1002", "activity-bob", "period-repo");

        fixtures.createContribution(user, repository, "activity-c-1002-jan", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-01-20T09:00:00Z"));
        fixtures.createContribution(user, repository, "activity-c-1002-feb-a", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-02-02T09:00:00Z"));
        fixtures.createContribution(user, repository, "activity-c-1002-feb-b", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-02-28T20:00:00Z"));
        fixtures.createContribution(user, repository, "activity-c-1002-mar", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-03-01T00:00:00Z"));

        fixtures.replaceActivityWeeks(user, repository, List.of(
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-01-19"), 1, 5, 1),
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-02-02"), 1, 11, 2),
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-02-23"), 1, 13, 4),
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-03-02"), 1, 17, 5)));

        given()
                .cookie(sessionCookie("activity-session-1002"))
                .queryParam("month", "2026-02")
        .when()
                .get("/api/me/activity")
        .then()
                .statusCode(200)
                .body("commitCount", equalTo(2))
                .body("activeProjects", equalTo(1))
                .body("additions", equalTo(24))
                .body("deletions", equalTo(6))
                .body("lineStatisticsCommitCount", equalTo(2))
                .body("firstActivityAt", equalTo("2026-02-02T09:00:00Z"))
                .body("lastActivityAt", equalTo("2026-02-28T20:00:00Z"))
                .body("commitsPerMonth", hasSize(1))
                .body("commitsPerMonth[0].month", equalTo("2026-02"))
                .body("commitsPerMonth[0].commits", equalTo(2))
                .body("commitsPerMonth[0].changedLines", equalTo(30));
    }

    @Test
    void repositorySearchScopeExcludesOtherRepositoriesFromAllAggregates() {
        AppUser user = fixtures.createUserWithSession(
                "activity-1003", "activity-carol", "Activity Carol", "activity-session-1003");
        SourceRepository included = fixtures.createRepository(
                user, "activity-repo-1003-a", "activity-carol", "target-service");
        SourceRepository excluded = fixtures.createRepository(
                user, "activity-repo-1003-b", "activity-carol", "other-service");

        fixtures.createContribution(user, included, "activity-c-1003-a", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-04-06T10:00:00Z"));
        fixtures.createContribution(user, excluded, "activity-c-1003-b", Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-04-07T10:00:00Z"));
        fixtures.replaceActivityWeeks(user, included, List.of(
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-04-06"), 1, 9, 1)));
        fixtures.replaceActivityWeeks(user, excluded, List.of(
                new TestFixtureService.ActivityWeekFixture(LocalDate.parse("2026-04-06"), 1, 90, 10)));

        given()
                .cookie(sessionCookie("activity-session-1003"))
                .queryParam("search", "target")
        .when()
                .get("/api/me/activity")
        .then()
                .statusCode(200)
                .body("commitCount", equalTo(1))
                .body("activeProjects", equalTo(1))
                .body("additions", equalTo(9))
                .body("deletions", equalTo(1))
                .body("lineStatisticsCommitCount", equalTo(1))
                .body("commitsPerYear[0].activeProjects", equalTo(1))
                .body("commitsPerYear[0].projects", hasSize(1))
                .body("commitsPerYear[0].projects[0]", equalTo("target-service"))
                .body("projectsOverTime", hasSize(1))
                .body("projectsOverTime[0].repositoryName", equalTo("target-service"));
    }

    private Cookie sessionCookie(String token) {
        return new Cookie.Builder(AuthenticationService.SESSION_COOKIE, token).build();
    }
}
