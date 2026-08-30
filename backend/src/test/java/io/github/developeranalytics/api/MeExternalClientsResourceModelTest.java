package io.github.developeranalytics.api;

import io.github.developeranalytics.domain.external.ExternalClientToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeExternalClientsResourceModelTest {

    @Test
    void createRequestRequiresExplicitScopesInContract() {
        var request = new MeExternalClientsResource.CreateRequest(
                "ChatGPT",
                Set.of(ExternalClientToken.Scope.PROFILE_READ),
                ExternalClientToken.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES
        );

        assertEquals("ChatGPT", request.name());
        assertEquals(
                Set.of(ExternalClientToken.Scope.PROFILE_READ),
                request.scopes()
        );
        assertEquals(
                ExternalClientToken.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES,
                request.privacyScope()
        );
    }
}
