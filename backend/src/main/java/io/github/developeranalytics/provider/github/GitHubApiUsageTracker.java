package io.github.developeranalytics.provider.github;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks GitHub REST requests performed by the current worker thread. */
@ApplicationScoped
public class GitHubApiUsageTracker {

    private final ThreadLocal<MutableUsage> current = new ThreadLocal<>();

    public Scope begin() {
        MutableUsage previous = current.get();
        MutableUsage usage = new MutableUsage();
        current.set(usage);
        return new Scope(this, previous, usage);
    }

    public void record(String endpoint) {
        MutableUsage usage = current.get();
        if (usage == null) return;
        String key = endpoint == null || endpoint.isBlank() ? "request" : endpoint;
        usage.total++;
        usage.byEndpoint.merge(key, 1, Integer::sum);
    }

    public UsageSnapshot snapshot() {
        MutableUsage usage = current.get();
        return usage == null ? UsageSnapshot.empty() : usage.snapshot();
    }

    private void close(MutableUsage previous, MutableUsage usage) {
        if (current.get() != usage) return;
        if (previous == null) current.remove(); else current.set(previous);
    }

    private static final class MutableUsage {
        int total;
        final Map<String, Integer> byEndpoint = new LinkedHashMap<>();

        UsageSnapshot snapshot() {
            return new UsageSnapshot(total, byEndpoint);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final GitHubApiUsageTracker owner;
        private final MutableUsage previous;
        private final MutableUsage usage;
        private boolean closed;

        private Scope(GitHubApiUsageTracker owner, MutableUsage previous, MutableUsage usage) {
            this.owner = owner;
            this.previous = previous;
            this.usage = usage;
        }

        public UsageSnapshot snapshot() {
            return usage.snapshot();
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            owner.close(previous, usage);
        }
    }

    public record UsageSnapshot(int totalRequests, Map<String, Integer> requestsByEndpoint) {
        public UsageSnapshot {
            requestsByEndpoint = Map.copyOf(requestsByEndpoint == null ? Map.of() : requestsByEndpoint);
        }

        static UsageSnapshot empty() {
            return new UsageSnapshot(0, Map.of());
        }
    }
}
