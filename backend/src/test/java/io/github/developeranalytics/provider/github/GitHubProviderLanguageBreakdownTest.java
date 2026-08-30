package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderLanguageBreakdown;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitHubProviderLanguageBreakdownTest {

    @Test
    void languageBreakdownIsImmutableAndKeepsMeasuredBytes() {
        ProviderLanguageBreakdown breakdown =
                new ProviderLanguageBreakdown(
                        Map.of("Java", 100L, "TypeScript", 25L),
                        null
                );

        assertEquals(100L, breakdown.bytesByLanguage().get("Java"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> breakdown.bytesByLanguage().put("Swift", 1L)
        );
    }
}
