package io.github.developeranalytics.domain.project;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RepositoryProjectCategoryId implements Serializable {
    public UUID repository;
    public UUID category;
    public RepositoryProjectCategory.Source source;

    public RepositoryProjectCategoryId() {}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RepositoryProjectCategoryId that)) return false;
        return Objects.equals(repository, that.repository)
                && Objects.equals(category, that.category)
                && source == that.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, category, source);
    }
}
