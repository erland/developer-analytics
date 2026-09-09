package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("github-adapter")
class GitHubContributorSnapshotServiceTest {

    @Test
    void derivesTotalsAndWeeklyActivityFromOneResponse() throws Exception {
        long weekStart = Instant.parse("2026-09-06T00:00:00Z").getEpochSecond();
        String body = """
                [
                  {"total":5,"author":{"login":"alice","type":"User"},"weeks":[
                    {"w":%d,"a":12,"d":3,"c":2}
                  ]},
                  {"total":4,"author":{"login":"dependabot[bot]","type":"Bot"},"weeks":[]}
                ]
                """.formatted(weekStart);

        StubAdapter adapter = new StubAdapter(body);
        GitHubContributorSnapshotService service = new GitHubContributorSnapshotService();
        service.github = adapter;
        service.mapper = new ObjectMapper();

        ProviderContributorSnapshot snapshot = service.fetch(
                new ProviderAccessToken("token"),
                repository(),
                "alice"
        );

        assertEquals(1, adapter.requestCount);
        assertEquals(2, snapshot.statistics().contributorCount());
        assertEquals(1, snapshot.statistics().humanContributorCount());
        assertEquals(1, snapshot.statistics().botContributorCount());
        assertEquals(5, snapshot.statistics().userCommitCount());
        assertEquals(9, snapshot.statistics().repositoryCommitCount());
        assertEquals(12, snapshot.statistics().userAdditions());
        assertEquals(3, snapshot.statistics().userDeletions());
        assertEquals(1, snapshot.userActivityWeeks().size());
        assertEquals(2, snapshot.userActivityWeeks().get(0).commits());
        assertEquals(12, snapshot.userActivityWeeks().get(0).additions());
        assertEquals(3, snapshot.userActivityWeeks().get(0).deletions());
    }

    private static ProviderRepository repository() {
        return new ProviderRepository(
                "1", "2", "owner", ProviderRepository.OwnerType.USER,
                "repo", "owner/repo", "https://github.com/owner/repo",
                ProviderRepository.Visibility.PUBLIC, false, false,
                null, null, null
        );
    }

    private static final class StubAdapter extends GitHubProviderAdapter {
        private final String responseBody;
        private int requestCount;

        private StubAdapter(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        HttpResponse<String> sendGet(URI uri, ProviderAccessToken accessToken) {
            requestCount++;
            return response(responseBody);
        }
    }

    private static HttpResponse<String> response(String body) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return 200; }
            @Override public HttpRequest request() { return HttpRequest.newBuilder(URI.create("https://api.github.com/test")).build(); }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
            @Override public String body() { return body; }
            @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return URI.create("https://api.github.com/test"); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
