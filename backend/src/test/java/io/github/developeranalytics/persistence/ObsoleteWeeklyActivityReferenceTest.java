package io.github.developeranalytics.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class ObsoleteWeeklyActivityReferenceTest {

    @Test
    void productionJavaCodeDoesNotReferenceRemovedWeeklyActivityInfrastructure() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> offenders;
        try (var paths = Files.walk(sourceRoot)) {
            offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsRemovedWeeklyReference(path))
                    .map(Path::toString)
                    .toList();
        }

        assertTrue(offenders.isEmpty(),
                () -> "Production code still references removed weekly activity infrastructure: " + offenders);
    }

    private boolean containsRemovedWeeklyReference(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("repository_user_activity_week")
                    || source.contains("RepositoryUserActivityWeekRepository")
                    || source.contains("GitHubWeeklyActivityService");
        } catch (IOException error) {
            throw new IllegalStateException("Unable to inspect " + path, error);
        }
    }
}
