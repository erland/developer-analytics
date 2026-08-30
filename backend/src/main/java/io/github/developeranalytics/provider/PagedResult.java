package io.github.developeranalytics.provider;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        String nextCursor,
        ProviderRateLimit rateLimit
) {
    public PagedResult {
        items = List.copyOf(items);
    }

    public boolean hasNextPage() {
        return nextCursor != null && !nextCursor.isBlank();
    }
}
