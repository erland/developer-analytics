package io.github.developeranalytics.service.technology;

import io.github.developeranalytics.domain.aggregate.TechnologyActivityMonth;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.persistence.technology.TechnologyTimelineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TechnologyTimelineService {

    @Inject
    TechnologyTimelineRepository timeline;

    @Transactional
    public int recalculate(AppUser user) {
        int updated = 0;

        for (var row : timeline.calculateActivitySource(user.getId())) {
            TechnologyActivityMonth month = timeline.findMonth(
                    user.getId(),
                    row.technologyKey(),
                    row.yearMonth()
            ).orElseGet(() -> {
                TechnologyActivityMonth created =
                        new TechnologyActivityMonth(
                                user,
                                row.technologyKey(),
                                row.yearMonth()
                        );
                timeline.persist(created);
                return created;
            });

            month.update(
                    row.repositoryCount(),
                    row.activityCount(),
                    io.github.developeranalytics.domain.model.DataPrivacyProvenance.fromRepositoryCounts(row.publicRepositoryCount(), row.privateRepositoryCount())
            );
            updated++;
        }

        return updated;
    }

    @Transactional
    public List<TechnologyTimeline> build(AppUser user) {
        Map<String, TechnologyTimelineRepository.EvidenceBoundsRow> bounds =
                timeline.evidenceBounds(user.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                TechnologyTimelineRepository.EvidenceBoundsRow::technologyKey,
                                row -> row
                        ));

        Map<String, Map<RepositoryVisibility, Integer>> visibility =
                new HashMap<>();

        for (var row : timeline.visibility(user.getId())) {
            visibility.computeIfAbsent(
                    row.technologyKey(),
                    ignored -> new EnumMap<>(RepositoryVisibility.class)
            ).put(row.visibility(), row.repositoryCount());
        }

        Map<String, List<TechnologyActivityMonth>> months =
                timeline.findMonthsForUser(user.getId())
                        .stream()
                        .collect(Collectors.groupingBy(
                                TechnologyActivityMonth::getTechnologyKey
                        ));

        Set<String> keys = new TreeSet<>();
        keys.addAll(bounds.keySet());
        keys.addAll(months.keySet());

        List<TechnologyTimeline> result = new ArrayList<>();

        for (String key : keys) {
            var bound = bounds.get(key);
            List<TechnologyActivityMonth> technologyMonths =
                    months.getOrDefault(key, List.of());

            Map<Integer, YearAccumulator> yearMap = new TreeMap<>();
            for (TechnologyActivityMonth month : technologyMonths) {
                YearAccumulator year = yearMap.computeIfAbsent(
                        month.getYearMonth().getYear(),
                        ignored -> new YearAccumulator()
                );
                year.activityCount += month.getActivityCount();
                year.maxRepositoryCount = Math.max(
                        year.maxRepositoryCount,
                        month.getRepositoryCount()
                );
            }

            List<YearPoint> years = yearMap.entrySet()
                    .stream()
                    .map(entry -> new YearPoint(
                            entry.getKey(),
                            entry.getValue().maxRepositoryCount,
                            entry.getValue().activityCount
                    ))
                    .toList();

            Map<RepositoryVisibility, Integer> vis =
                    visibility.getOrDefault(key, Map.of());

            result.add(new TechnologyTimeline(
                    key,
                    bound == null ? null : bound.firstObservedAt(),
                    bound == null ? null : bound.lastObservedAt(),
                    bound == null ? 0 : bound.repositoryCount(),
                    vis.getOrDefault(RepositoryVisibility.PUBLIC, 0),
                    vis.getOrDefault(RepositoryVisibility.PRIVATE, 0),
                    years
            ));
        }

        return result;
    }

    private static class YearAccumulator {
        int maxRepositoryCount;
        int activityCount;
    }

    public record YearPoint(
            int year,
            int projectCount,
            int activityCount
    ) {}

    public record TechnologyTimeline(
            String technologyKey,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            int observedRepositoryCount,
            int publicRepositoryCount,
            int privateRepositoryCount,
            List<YearPoint> years
    ) {}
}
