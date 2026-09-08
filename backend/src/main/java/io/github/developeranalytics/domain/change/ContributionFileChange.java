package io.github.developeranalytics.domain.change;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted statistics for one changed file within one contribution.
 *
 * <p>The row deliberately stores only file-level metadata and line counts; patch/source
 * contents are not retained. User, repository and occurrence time are repeated from the
 * owning contribution to support bounded activity aggregation by time and change kind.</p>
 */
@Entity
@Table(
        name = "contribution_file_change",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_contribution_file_change_path",
                columnNames = {"contribution_id", "path"}
        )
)
public class ContributionFileChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contribution_id", nullable = false)
    private Contribution contribution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_repository_id", nullable = false)
    private SourceRepository repository;

    @Column(nullable = false, length = 4096)
    private String path;

    @Column(nullable = false)
    private int additions;

    @Column(nullable = false)
    private int deletions;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_kind", nullable = false, length = 32)
    private ChangeKind changeKind;

    @Column(name = "classifier_confidence", nullable = false)
    private double classifierConfidence;

    @Column(name = "classifier_rule_key", nullable = false, length = 128)
    private String classifierRuleKey;

    @Column(name = "classifier_version", nullable = false, length = 64)
    private String classifierVersion;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_provenance", nullable = false, length = 32)
    private DataPrivacyProvenance privacyProvenance;

    protected ContributionFileChange() {}

    public ContributionFileChange(
            Contribution contribution,
            AppUser user,
            SourceRepository repository,
            String path,
            int additions,
            int deletions,
            ChangeKindClassification classification,
            OffsetDateTime occurredAt
    ) {
        this.contribution = Objects.requireNonNull(contribution, "contribution must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.path = requireNonBlank(path, "path");
        if (additions < 0) throw new IllegalArgumentException("additions must not be negative");
        if (deletions < 0) throw new IllegalArgumentException("deletions must not be negative");
        this.additions = additions;
        this.deletions = deletions;

        ChangeKindClassification result = Objects.requireNonNull(classification, "classification must not be null");
        this.changeKind = result.kind();
        this.classifierConfidence = result.confidence();
        this.classifierRuleKey = result.ruleKey();
        this.classifierVersion = result.classifierVersion();
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.privacyProvenance = DataPrivacyProvenance.fromVisibility(repository.getVisibility());
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value;
    }

    public UUID getId() { return id; }
    public Contribution getContribution() { return contribution; }
    public AppUser getUser() { return user; }
    public SourceRepository getRepository() { return repository; }
    public String getPath() { return path; }
    public int getAdditions() { return additions; }
    public int getDeletions() { return deletions; }
    public ChangeKind getChangeKind() { return changeKind; }
    public double getClassifierConfidence() { return classifierConfidence; }
    public String getClassifierRuleKey() { return classifierRuleKey; }
    public String getClassifierVersion() { return classifierVersion; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public DataPrivacyProvenance getPrivacyProvenance() { return privacyProvenance; }
}
