package io.github.developeranalytics.service.change;

import io.github.developeranalytics.domain.change.ChangeKind;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared parsing/normalisation for the changeKinds query semantic.
 *
 * <p>An empty selection means all activity (backwards-compatible behaviour). Values may be
 * supplied as repeated query parameters or comma-separated lists.</p>
 */
public final class ChangeKindSelection {
    private ChangeKindSelection() {}

    public static Set<ChangeKind> parse(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) return Set.of();
        EnumSet<ChangeKind> result = EnumSet.noneOf(ChangeKind.class);
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) continue;
            for (String part : raw.split(",")) {
                String value = part.trim();
                if (value.isEmpty()) continue;
                try {
                    result.add(ChangeKind.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_')));
                } catch (IllegalArgumentException error) {
                    throw new IllegalArgumentException("Unknown change kind: " + value, error);
                }
            }
        }
        return result.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(result));
    }

    public static boolean isAll(Set<ChangeKind> kinds) {
        return kinds == null || kinds.isEmpty();
    }
}
