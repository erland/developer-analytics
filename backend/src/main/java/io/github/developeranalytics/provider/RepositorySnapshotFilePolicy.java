package io.github.developeranalytics.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Shared bounds and path rules for repository file/manifest evidence snapshots. */
public final class RepositorySnapshotFilePolicy {

    public static final int MAX_RELEVANT_FILES = 40;
    public static final long MAX_FILE_BYTES = 1_048_576L;

    private RepositorySnapshotFilePolicy() {}

    public static boolean isRelevantTechnologyFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return false;
        String path = rawPath.toLowerCase(Locale.ROOT);
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return name.equals("pom.xml") || name.equals("package.json") || name.equals("dockerfile") ||
                name.equals("docker-compose.yml") || name.equals("docker-compose.yaml") || name.equals("compose.yml") ||
                name.equals("compose.yaml") || name.equals("package.swift") || name.equals("pyproject.toml") ||
                name.equals("requirements.txt") || name.equals(".terraform.lock.hcl") || name.endsWith(".tf") ||
                name.equals("chart.yaml") || name.equals("kustomization.yaml") || name.equals("androidmanifest.xml") ||
                name.equals("project.pbxproj") || name.equals("platformio.ini") || name.endsWith(".ino") ||
                path.startsWith(".github/workflows/") || path.contains("/.github/workflows/") ||
                path.startsWith("db/migration/") || path.contains("/db/migration/");
    }

    public static boolean isReadableFileSize(long sizeBytes) {
        return sizeBytes >= 0 && sizeBytes <= MAX_FILE_BYTES;
    }

    public static List<String> selectRelevantPaths(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (String rawPath : paths) {
            if (rawPath == null) continue;
            String path = rawPath.strip();
            if (path.isEmpty() || !isRelevantTechnologyFile(path)) continue;
            result.add(path);
            if (result.size() >= MAX_RELEVANT_FILES) break;
        }
        return List.copyOf(result);
    }
}
