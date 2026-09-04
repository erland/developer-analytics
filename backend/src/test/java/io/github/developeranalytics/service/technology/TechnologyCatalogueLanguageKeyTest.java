package io.github.developeranalytics.service.technology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class TechnologyCatalogueLanguageKeyTest {

    @Test
    void normalizesKnownGitHubLanguageNamesToStableTechnologyKeys() {
        assertEquals("cpp", TechnologyCatalogueService.languageTechnologyKey("C++"));
        assertEquals("csharp", TechnologyCatalogueService.languageTechnologyKey("C#"));
        assertEquals("objective-c", TechnologyCatalogueService.languageTechnologyKey("Objective-C"));
        assertEquals("objective-cpp", TechnologyCatalogueService.languageTechnologyKey("Objective-C++"));
        assertEquals("perl", TechnologyCatalogueService.languageTechnologyKey("Perl"));
        assertEquals("lua", TechnologyCatalogueService.languageTechnologyKey("Lua"));
    }

    @Test
    void derivesKeysForPreviouslyUnknownGitHubLanguages() {
        assertEquals("elixir", TechnologyCatalogueService.languageTechnologyKey("Elixir"));
        assertEquals("common-lisp", TechnologyCatalogueService.languageTechnologyKey("Common Lisp"));
    }
}
