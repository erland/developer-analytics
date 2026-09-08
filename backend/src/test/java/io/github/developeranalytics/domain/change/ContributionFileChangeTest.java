package io.github.developeranalytics.domain.change;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ContributionFileChangeTest {

    @Test
    void representsMixedCommitWithoutCollapsingChangeKinds() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-1", "owner", "sample");
        OffsetDateTime occurredAt = OffsetDateTime.of(2026, 9, 8, 1, 0, 0, 0, ZoneOffset.UTC);
        Contribution contribution = new Contribution(
                user,
                repository,
                "github",
                "abc123",
                Contribution.Type.COMMIT,
                occurredAt
        );

        ContributionFileChange code = new ContributionFileChange(
                contribution,
                user,
                repository,
                "src/main/App.java",
                12,
                3,
                new ChangeKindClassification(ChangeKind.CODE, 1.0, "extension-java", "1"),
                occurredAt
        );
        ContributionFileChange documentation = new ContributionFileChange(
                contribution,
                user,
                repository,
                "docs/guide.md",
                40,
                2,
                new ChangeKindClassification(ChangeKind.DOCUMENTATION, 1.0, "extension-md", "1"),
                occurredAt
        );

        assertSame(contribution, code.getContribution());
        assertSame(contribution, documentation.getContribution());
        assertEquals(ChangeKind.CODE, code.getChangeKind());
        assertEquals(ChangeKind.DOCUMENTATION, documentation.getChangeKind());
        assertEquals(15, code.getAdditions() + code.getDeletions());
        assertEquals(42, documentation.getAdditions() + documentation.getDeletions());
    }

    @Test
    void inheritsPrivatePrivacyProvenanceFromRepository() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-2", "owner", "private-sample");
        repository.setVisibility(RepositoryVisibility.PRIVATE);
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        Contribution contribution = new Contribution(
                user,
                repository,
                "github",
                "def456",
                Contribution.Type.COMMIT,
                occurredAt
        );

        ContributionFileChange change = new ContributionFileChange(
                contribution,
                user,
                repository,
                "README.md",
                1,
                0,
                ChangeKindClassification.otherFallback("1"),
                occurredAt
        );

        assertEquals(DataPrivacyProvenance.PRIVATE_AGGREGATE, change.getPrivacyProvenance());
    }

    @Test
    void rejectsInvalidFileStatistics() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-3", "owner", "sample");
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        Contribution contribution = new Contribution(
                user,
                repository,
                "github",
                "ghi789",
                Contribution.Type.COMMIT,
                occurredAt
        );
        ChangeKindClassification classification = ChangeKindClassification.otherFallback("1");

        assertThrows(IllegalArgumentException.class, () -> new ContributionFileChange(
                contribution, user, repository, "file.txt", -1, 0, classification, occurredAt));
        assertThrows(IllegalArgumentException.class, () -> new ContributionFileChange(
                contribution, user, repository, "file.txt", 0, -1, classification, occurredAt));
        assertThrows(IllegalArgumentException.class, () -> new ContributionFileChange(
                contribution, user, repository, "   ", 0, 0, classification, occurredAt));
    }
}
