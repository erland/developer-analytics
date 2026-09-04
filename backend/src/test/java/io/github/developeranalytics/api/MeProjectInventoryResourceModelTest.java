package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class MeProjectInventoryResourceModelTest {

    @Test
    void inventoryResponseSupportsPagingAndMultiValueMetadata() {
        var item = new MeProjectInventoryResource.Item(
                UUID.randomUUID(),
                "demo",
                "Demo project",
                "https://github.com/example/demo",
                "EXTERNAL",
                "PUBLIC",
                OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                123_456L,
                789_012L,
                List.of(
                        new MeProjectInventoryResource.Category(
                                "backend-service",
                                "Backend service"
                        )
                ),
                List.of(
                        new MeProjectInventoryResource.Technology(
                                "java",
                                "Java"
                        )
                )
        );

        var response = new MeProjectInventoryResource.Response(
                List.of(item),
                51,
                1,
                25,
                3,
                new MeProjectInventoryResource.Facets(
                        List.of(new MeProjectInventoryResource.FacetValue("java", "Java", 42)),
                        List.of(new MeProjectInventoryResource.FacetValue("backend-service", "Backend service", 17)),
                        List.of(new MeProjectInventoryResource.FacetValue("own", "Own", 12))
                )
        );

        assertEquals(3, response.totalPages());
        assertEquals(123_456L, response.items().getFirst().codeSizeBytes());
        assertEquals(789_012L, response.items().getFirst().repositorySizeBytes());
        assertEquals("Backend service", response.items().getFirst().categories().getFirst().name());
        assertEquals("Java", response.items().getFirst().technologies().getFirst().name());
        assertEquals(42, response.facets().technologies().getFirst().count());
        assertEquals("Backend service", response.facets().projectTypes().getFirst().name());
        assertEquals("own", response.facets().ownership().getFirst().key());
    }
}
