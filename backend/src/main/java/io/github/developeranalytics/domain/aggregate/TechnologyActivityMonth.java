package io.github.developeranalytics.domain.aggregate;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "technology_activity_month")
public class TechnologyActivityMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "technology_key", nullable = false, length = 128)
    private String technologyKey;

    @Column(name = "year_month", nullable = false)
    private LocalDate yearMonth;

    @Column(name = "repository_count", nullable = false)
    private int repositoryCount;

    @Column(name = "activity_count", nullable = false)
    private int activityCount;

    protected TechnologyActivityMonth() {}

    public TechnologyActivityMonth(
            AppUser user,
            String technologyKey,
            LocalDate yearMonth
    ) {
        this.user = user;
        this.technologyKey = technologyKey;
        this.yearMonth = yearMonth;
    }

    public void update(int repositoryCount, int activityCount) {
        this.repositoryCount = repositoryCount;
        this.activityCount = activityCount;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTechnologyKey() { return technologyKey; }
    public LocalDate getYearMonth() { return yearMonth; }
    public int getRepositoryCount() { return repositoryCount; }
    public int getActivityCount() { return activityCount; }
}
