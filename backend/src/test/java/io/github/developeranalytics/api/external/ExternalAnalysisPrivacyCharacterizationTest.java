package io.github.developeranalytics.api.external;

import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.support.TestFixtureService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@Tag("authorization")
@Tag("privacy")
class ExternalAnalysisPrivacyCharacterizationTest {

    @Inject TestFixtureService fixtures;

    @Test
    void publicOnlyExcludesPrivateProjectsAndPrivateAggregates() {
        Scenario scenario = scenario("public-only", ExternalClientToken.PrivacyScope.PUBLIC_ONLY);

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/projects")
                .then().statusCode(200)
                .contentType(ExternalAnalysisMediaType.VALUE)
                .body("size()", is(1))
                .body("name", contains("public-repo"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/profile")
                .then().statusCode(200)
                .body("repositoryCount", is(1))
                .body("publicRepositoryCount", is(1))
                .body("privateRepositoryCount", is(0))
                .body("contributionCount", is(1))
                .body("privacyProvenance", is("PUBLIC_ONLY"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/activity?months=24")
                .then().statusCode(200)
                .body("contributionCount", is(1))
                .body("activeProjectCount", is(1))
                .body("privacyProvenance", is("PUBLIC_ONLY"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/contributions")
                .then().statusCode(200)
                .body("total", is(1))
                .body("privacyProvenance", is("PUBLIC_ONLY"));
    }

    @Test
    void aggregateScopeIncludesPrivateAggregatesButStillHidesPrivateProjectDetail() {
        Scenario scenario = scenario("aggregate", ExternalClientToken.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES);

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/projects")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("name", contains("public-repo"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/profile")
                .then().statusCode(200)
                .body("repositoryCount", is(2))
                .body("publicRepositoryCount", is(1))
                .body("privateRepositoryCount", is(1))
                .body("contributionCount", is(2))
                .body("privacyProvenance", is("INCLUDES_PRIVATE"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/activity?months=24")
                .then().statusCode(200)
                .body("contributionCount", is(2))
                .body("activeProjectCount", is(2))
                .body("privacyProvenance", is("INCLUDES_PRIVATE"));

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/contributions")
                .then().statusCode(200)
                .body("total", is(2))
                .body("privacyProvenance", is("INCLUDES_PRIVATE"));
    }

    @Test
    void fullScopeIncludesPrivateProjectDetail() {
        Scenario scenario = scenario("full", ExternalClientToken.PrivacyScope.FULL_AUTHORISED_ANALYSIS);

        given().header("Authorization", "Bearer " + scenario.rawToken())
                .accept(ExternalAnalysisMediaType.VALUE)
                .when().get("/api/me/projects")
                .then().statusCode(200)
                .body("size()", is(2))
                .body("name", containsInAnyOrder("public-repo", "private-repo"));
    }

    private Scenario scenario(String suffix, ExternalClientToken.PrivacyScope privacyScope) {
        String rawToken = "external-" + suffix + "-" + UUID.randomUUID();
        AppUser user = fixtures.createUserWithSession(
                "external-" + suffix + "-" + UUID.randomUUID(),
                "user-" + suffix,
                "User " + suffix,
                "session-" + UUID.randomUUID());

        SourceRepository publicRepository = fixtures.createRepository(
                user, "public-" + UUID.randomUUID(), "owner", "public-repo");
        SourceRepository privateRepository = fixtures.createRepository(
                user, "private-" + UUID.randomUUID(), "owner", "private-repo");
        fixtures.setRepositoryVisibility(privateRepository, RepositoryVisibility.PRIVATE);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        fixtures.createContribution(user, publicRepository, "public-c-" + UUID.randomUUID(), Contribution.Type.COMMIT, now);
        fixtures.createContribution(user, privateRepository, "private-c-" + UUID.randomUUID(), Contribution.Type.PULL_REQUEST, now.plusHours(1));

        fixtures.createExternalClientToken(
                user,
                "test-" + suffix,
                rawToken,
                Set.of(
                        ExternalClientToken.Scope.PROFILE_READ,
                        ExternalClientToken.Scope.PROJECTS_READ,
                        ExternalClientToken.Scope.ACTIVITY_READ,
                        ExternalClientToken.Scope.CONTRIBUTIONS_READ),
                privacyScope);
        return new Scenario(rawToken);
    }

    private record Scenario(String rawToken) {}
}
