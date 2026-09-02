package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeProjectDetailResourceModelTest {

    @Test
    void detailKeepsSignificanceAndInvolvementSeparate() {
        var detail = new MeProjectDetailResource.Detail(
                new MeProjectDetailResource.Metadata(
                        UUID.randomUUID(),
                        "github",
                        "demo",
                        "alice/demo",
                        "Demo",
                        "https://github.com/alice/demo",
                        "PUBLIC",
                        "OWNED_BY_USER",
                        "alice",
                        false,
                        false,
                        List.of("api"),
                        OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                        false
                ),
                new MeProjectDetailResource.Activity(
                        10, 2, 1, 1, 100, 40,
                        OffsetDateTime.parse("2026-01-01T08:00:00Z"),
                        OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                        List.of(new MeProjectDetailResource.ActivityPoint("2026-08", 4, 140, 4))
                ),
                List.of(),
                List.of(),
                new MeProjectDetailResource.Assessment(
                        "HIGH",
                        70,
                        Map.of("activityScore", 20),
                        "VERY_HIGH",
                        85,
                        Map.of("contributionScore", 35),
                        OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                        "PUBLIC_ONLY"
                ),
                new MeProjectDetailResource.Synchronisation(
                        "SYNCED",
                        OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                        null
                ),
                new MeProjectDetailResource.Contributors(4, 3, 1, 10)
        );

        assertEquals(70, detail.assessment().significanceScore());
        assertEquals(85, detail.assessment().involvementScore());
        assertEquals(140, detail.activity().timeline().getFirst().changedLines());
        assertEquals("SYNCED", detail.synchronisation().status());
    }
}
