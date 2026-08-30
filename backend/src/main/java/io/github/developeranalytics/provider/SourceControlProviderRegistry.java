package io.github.developeranalytics.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Locale;

@ApplicationScoped
public class SourceControlProviderRegistry {

    @Inject
    Instance<SourceControlProvider> providers;

    public SourceControlProvider require(String providerKey) {
        String normalized = providerKey.trim().toLowerCase(Locale.ROOT);
        for (SourceControlProvider provider : providers) {
            if (provider.providerKey().equals(normalized)) return provider;
        }
        throw new IllegalArgumentException("Unsupported source control provider: " + providerKey);
    }
}
