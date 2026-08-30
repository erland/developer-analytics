package io.github.developeranalytics.domain.external;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalClientPrivacyScopeTest {

    @Test
    void publicOnlyAllowsNoPrivateData() {
        var scope = ExternalClientToken.PrivacyScope.PUBLIC_ONLY;
        assertFalse(scope.allowsPrivateAggregates());
        assertFalse(scope.allowsPrivateProjectDetail());
    }

    @Test
    void aggregateScopeAllowsAggregatesButNotProjectDetail() {
        var scope = ExternalClientToken.PrivacyScope
                .PUBLIC_PLUS_PRIVATE_AGGREGATES;
        assertTrue(scope.allowsPrivateAggregates());
        assertFalse(scope.allowsPrivateProjectDetail());
    }

    @Test
    void fullScopeAllowsAuthorisedPrivateDetail() {
        var scope = ExternalClientToken.PrivacyScope
                .FULL_AUTHORISED_ANALYSIS;
        assertTrue(scope.allowsPrivateAggregates());
        assertTrue(scope.allowsPrivateProjectDetail());
    }
}
