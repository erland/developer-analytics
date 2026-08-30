package io.github.developeranalytics.ai;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;

@ApplicationScoped
public class AiPrivacyPolicyService {

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "developer-analytics.ai.private-data-policy")
    AiPrivacyPolicy providerPolicy;

    public Decision evaluate(AiRequestContext context) {
        AppUser user = entityManager.find(
                AppUser.class,
                context.userId()
        );

        if (user == null) {
            return Decision.denied("unknown-user");
        }

        if (context.sensitivity() == AiDataSensitivity.PUBLIC_DATA) {
            return providerPolicy == AiPrivacyPolicy.PRIVATE_AI_DISABLED
                    ? Decision.denied("provider-policy-disables-ai-for-private-mode")
                    : Decision.permit();
        }

        if (context.sensitivity() == AiDataSensitivity.PRIVATE_CONTENT) {
            return Decision.denied(
                    "private-repository-content-is-not-allowed"
            );
        }

        if (providerPolicy != AiPrivacyPolicy.PRIVATE_METADATA_ALLOWED) {
            return Decision.denied(
                    "provider-policy-does-not-allow-private-metadata"
            );
        }

        if (user.getAiPrivacyPolicy() !=
                AiPrivacyPolicy.PRIVATE_METADATA_ALLOWED) {
            return Decision.denied(
                    "user-has-not-consented-to-private-metadata-ai"
            );
        }

        return Decision.permit();
    }

    public record Decision(
            boolean allowed,
            String reason
    ) {
        public static Decision permit() {
            return new Decision(true, "allowed");
        }

        public static Decision denied(String reason) {
            return new Decision(false, reason);
        }
    }
}
