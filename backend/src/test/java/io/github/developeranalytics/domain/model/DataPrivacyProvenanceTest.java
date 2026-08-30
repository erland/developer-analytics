package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class DataPrivacyProvenanceTest {
    @Test void publicOnly() { assertEquals(DataPrivacyProvenance.PUBLIC_ONLY, DataPrivacyProvenance.fromRepositoryCounts(4,0)); }
    @Test void includesPrivate() { assertEquals(DataPrivacyProvenance.INCLUDES_PRIVATE, DataPrivacyProvenance.fromRepositoryCounts(4,2)); }
    @Test void privateAggregate() { assertEquals(DataPrivacyProvenance.PRIVATE_AGGREGATE, DataPrivacyProvenance.fromRepositoryCounts(0,2)); }
    @Test void mapsVisibility() {
        assertEquals(DataPrivacyProvenance.PUBLIC_ONLY, DataPrivacyProvenance.fromVisibility(RepositoryVisibility.PUBLIC));
        assertEquals(DataPrivacyProvenance.PRIVATE_AGGREGATE, DataPrivacyProvenance.fromVisibility(RepositoryVisibility.PRIVATE));
    }
}
