package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("github-adapter")
class GitHubProviderAdapterTest {

    private GitHubProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GitHubProviderAdapter();
        adapter.mapper = new ObjectMapper();
    }

    @Test
    void mapsRepositoryIntoProviderNeutralModel() throws Exception {
        String body = "{" +
                "\"id\":42," +
                "\"name\":\"developer-analytics\"," +
                "\"full_name\":\"erland/developer-analytics\"," +
                "\"html_url\":\"https://github.com/erland/developer-analytics\"," +
                "\"private\":true," +
                "\"fork\":false," +
                "\"archived\":false," +
                "\"size\":1234," +
                "\"created_at\":\"2026-08-01T10:00:00Z\"," +
                "\"updated_at\":\"2026-08-30T07:00:00Z\"," +
                "\"pushed_at\":\"2026-08-30T06:59:00Z\"," +
                "\"owner\":{\"id\":99,\"login\":\"erland\",\"type\":\"User\"}" +
                "}";

        ProviderRepository repository = adapter.mapRepository(adapter.mapper.readTree(body));

        assertEquals("42", repository.externalRepositoryId());
        assertEquals("99", repository.ownerExternalId());
        assertEquals("erland", repository.ownerLogin());
        assertEquals(ProviderRepository.OwnerType.USER, repository.ownerType());
        assertEquals(ProviderRepository.Visibility.PRIVATE, repository.visibility());
        assertEquals("developer-analytics", repository.name());
        assertEquals(1234L * 1024L, repository.repositorySizeBytes());
        assertFalse(repository.fork());
        assertFalse(repository.archived());
    }

    @Test
    void accessTokenDoesNotLeakThroughToString() {
        assertEquals("[REDACTED]", new io.github.developeranalytics.provider.ProviderAccessToken("secret").toString());
    }
}
