package io.github.developeranalytics.provider.history;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.provider.ProviderRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ContributionHistoryProviderTest {

    @Test
    void resultRequiresCommitShaAndDefensivelyCopiesFileChanges() {
        var source = new java.util.ArrayList<ProviderContributionFileChange>();
        source.add(new ProviderContributionFileChange("src/App.java", 12, 3));

        var result = new HistoricalCommitFileChanges("abc123", source);
        source.clear();

        assertEquals("abc123", result.commitSha());
        assertEquals(1, result.fileChanges().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.fileChanges().add(new ProviderContributionFileChange("README.md", 1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricalCommitFileChanges(" ", List.of()));
    }

    @Test
    void contractCanReturnHistoricalChangesWithoutExposingTransport() throws Exception {
        ContributionHistoryProvider provider = new ContributionHistoryProvider() {
            @Override
            public List<HistoricalCommitFileChanges> fetchFileChanges(
                    ProviderAccessToken accessToken,
                    ProviderRepository repository,
                    Collection<String> commitShas
            ) {
                return List.of(new HistoricalCommitFileChanges(
                        commitShas.iterator().next(),
                        List.of(new ProviderContributionFileChange("docs/guide.md", 5, 1))));
            }
        };

        var result = provider.fetchFileChanges(
                new ProviderAccessToken("token"),
                repository(),
                List.of("deadbeef"));

        assertEquals("deadbeef", result.getFirst().commitSha());
        assertEquals("docs/guide.md", result.getFirst().fileChanges().getFirst().path());
    }

    private ProviderRepository repository() {
        return new ProviderRepository(
                "1", "owner-id", "owner", ProviderRepository.OwnerType.USER,
                "repo", "owner/repo", "https://github.com/owner/repo",
                ProviderRepository.Visibility.PUBLIC, false, false,
                null, null, null, null, List.of(), null);
    }
}
