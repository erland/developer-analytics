package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.technology.LanguageEvidenceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GitHubLanguageEvidenceJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_LANGUAGE_EVIDENCE";

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    LanguageEvidenceService evidence;

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        Object repositoryId = job.getPayload().get("repositoryId");
        if (job.getUser() == null || repositoryId == null) {
            throw new IllegalStateException(
                    "Language evidence job requires user and repositoryId"
            );
        }

        SourceRepository repository = repositories.findByIdForUser(
                UUID.fromString(repositoryId.toString()),
                job.getUser().getId()
        ).orElseThrow(() ->
                new IllegalStateException("Repository not found for job user")
        );

        evidence.collect(job.getUser(), repository);
    }
}
