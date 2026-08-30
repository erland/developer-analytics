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
                3
        );

        assertEquals(3, response.totalPages());
        assertEquals("Backend service", response.items().getFirst().categories().getFirst().name());
        assertEquals("Java", response.items().getFirst().technologies().getFirst().name());
    }
}
