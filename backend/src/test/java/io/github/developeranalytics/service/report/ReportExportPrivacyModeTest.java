package io.github.developeranalytics.service.report;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class ReportExportPrivacyModeTest {

    @Test
    void exposesThreeExplicitPrivateDataModes() {
        assertArrayEquals(
                new ReportExportService.PrivateDataMode[] {
                        ReportExportService.PrivateDataMode.EXCLUDE_PRIVATE,
                        ReportExportService.PrivateDataMode.INCLUDE_PRIVATE_AGGREGATES,
                        ReportExportService.PrivateDataMode.INCLUDE_FULL_PRIVATE_DETAIL
                },
                ReportExportService.PrivateDataMode.values()
        );
    }

    @Test
    void noImplicitDefaultPrivateDataModeExists() {
        assertThrows(
                NullPointerException.class,
                () -> java.util.Objects.requireNonNull(
                        null,
                        "privateDataMode must be explicitly selected"
                )
        );
    }
}
