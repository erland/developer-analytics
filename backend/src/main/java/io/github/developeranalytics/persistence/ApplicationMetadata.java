package io.github.developeranalytics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "application_metadata")
public class ApplicationMetadata {

    @Id
    @Column(name = "metadata_key", nullable = false, length = 100)
    private String key;

    @Column(name = "metadata_value", nullable = false)
    private String value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ApplicationMetadata() {
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
