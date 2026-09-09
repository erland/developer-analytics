package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;
import io.github.developeranalytics.provider.ProviderContributorActivityWeek;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GitHubWeeklyActivityService {

    @Inject RepositoryUserActivityWeekRepository weeks;

    /** Persist weekly activity already obtained from the shared contributor statistics response. */
    @Transactional
    public void replace(UUID userId, SourceRepository repository, List<ProviderContributorActivityWeek> activity) {
        List<RepositoryUserActivityWeekRepository.WeekInput> inputs = activity == null
                ? List.of()
                : activity.stream()
                        .map(week -> new RepositoryUserActivityWeekRepository.WeekInput(
                                week.weekStart(), week.commits(), week.additions(), week.deletions()))
                        .toList();
        weeks.replace(userId, repository.getId(), inputs, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
