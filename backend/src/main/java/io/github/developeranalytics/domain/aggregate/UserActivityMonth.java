package io.github.developeranalytics.domain.aggregate;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="user_activity_month")
public class UserActivityMonth {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Column(name="year_month", nullable=false) private LocalDate yearMonth;
    @Column(name="commit_count", nullable=false) private int commitCount;
    @Column(nullable=false) private long additions;
    @Column(nullable=false) private long deletions;
    @Column(name="changed_lines", nullable=false) private long changedLines;
    @Column(name="active_repository_count", nullable=false) private int activeRepositoryCount;
    @Column(name="pull_request_count", nullable=false) private int pullRequestCount;
    @Column(name="review_count", nullable=false) private int reviewCount;
    @Column(name="issue_count", nullable=false) private int issueCount;
    @Column(name="release_count", nullable=false) private int releaseCount;
    @Column(name="maintenance_count", nullable=false) private int maintenanceCount;
    @Enumerated(EnumType.STRING)
    @Column(name="privacy_provenance", nullable=false, length=32)
    private DataPrivacyProvenance privacyProvenance = DataPrivacyProvenance.PUBLIC_ONLY;
    protected UserActivityMonth() {}
    public UserActivityMonth(AppUser user, LocalDate yearMonth) { this.user=user; this.yearMonth=yearMonth; }
    public DataPrivacyProvenance getPrivacyProvenance() { return privacyProvenance; }
}
