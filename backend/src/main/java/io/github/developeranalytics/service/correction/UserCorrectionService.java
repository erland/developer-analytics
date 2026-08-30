package io.github.developeranalytics.service.correction;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.correction.UserAnalysisCorrectionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class UserCorrectionService {

    @Inject
    UserAnalysisCorrectionRepository corrections;

    @Transactional
    public void set(
            AppUser user,
            SourceRepository repository,
            UserAnalysisCorrection.Type type,
            String correctionKey,
            boolean enabled
    ) {
        String key = normalize(correctionKey);

        var existing = corrections.find(
                user.getId(),
                repository == null ? null : repository.getId(),
                type,
                key
        );

        if (enabled && existing.isEmpty()) {
            corrections.persist(new UserAnalysisCorrection(
                    user,
                    repository,
                    type,
                    key
            ));
        } else if (!enabled && existing.isPresent()) {
            corrections.delete(existing.get());
        }
    }

    public boolean isProjectCategoryRejected(
            UUID userId,
            UUID repositoryId,
            String categoryKey
    ) {
        return corrections.exists(
                userId,
                repositoryId,
                UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED,
                categoryKey
        );
    }

    public boolean isTechnologySuppressed(
            UUID userId,
            String technologyKey
    ) {
        return corrections.exists(
                userId,
                null,
                UserAnalysisCorrection.Type.TECHNOLOGY_INFERENCE_SUPPRESSED,
                technologyKey
        );
    }

    public boolean isProjectExcludedFromAiProfile(
            UUID userId,
            UUID repositoryId
    ) {
        return corrections.exists(
                userId,
                repositoryId,
                UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE,
                null
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
