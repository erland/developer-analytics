package io.github.developeranalytics.auth;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderIdentity;

public record CurrentUser(AppUser user, ProviderIdentity identity) {
}
