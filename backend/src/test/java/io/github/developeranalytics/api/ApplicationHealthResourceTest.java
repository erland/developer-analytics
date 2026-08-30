package io.github.developeranalytics.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@Tag("persistence")
class ApplicationHealthResourceTest {

    @Test
    void applicationHealthEndpointReturnsUp() {
        given()
          .when().get("/api/health/application")
          .then()
             .statusCode(200)
             .body("status", equalTo("UP"))
             .body("service", equalTo("developer-analytics-backend"));
    }
}
