package io.github.developeranalytics.provider;

import java.util.Collections;
import java.util.List;

public record ProviderRepositorySnapshot(
        List<ProviderRepositoryFile> files,
        ProviderRateLimit rateLimit
) {
    public ProviderRepositorySnapshot {
        files = files == null ? List.of() : Collections.unmodifiableList(files);
    }
}
