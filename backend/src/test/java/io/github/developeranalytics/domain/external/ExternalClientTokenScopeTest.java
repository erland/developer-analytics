package io.github.developeranalytics.domain.external;

import io.github.developeranalytics.domain.model.AppUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExternalClientTokenScopeTest {

    @Test
    void tokenIsScopedAndRevocable() {
        ExternalClientToken token = new ExternalClientToken(
                AppUser.create(),
                "GPT",
                "hash",
                Set.of(
                        ExternalClientToken.Scope.PROFILE_READ,
                        ExternalClientToken.Scope.PROJECTS_READ
                ),
                ExternalClientToken.PrivacyScope.PUBLIC_ONLY
        );

        assertTrue(token.hasScope(
                ExternalClientToken.Scope.PROFILE_READ
        ));
        assertFalse(token.hasScope(
                ExternalClientToken.Scope.EVIDENCE_READ
        ));
        assertFalse(token.isRevoked());
        assertEquals(
                ExternalClientToken.PrivacyScope.PUBLIC_ONLY,
                token.getPrivacyScope()
        );
        assertFalse(token.getPrivacyScope().allowsPrivateAggregates());
        assertFalse(token.getPrivacyScope().allowsPrivateProjectDetail());

        token.revoke();

        assertTrue(token.isRevoked());
        assertNotNull(token.getRevokedAt());
    }
}
