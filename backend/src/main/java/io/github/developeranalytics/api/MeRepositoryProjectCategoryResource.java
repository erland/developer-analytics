package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.project.RepositoryProjectCategory;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/me/repositories/{repositoryId}/project-categories")
@Produces(MediaType.APPLICATION_JSON)
public class MeRepositoryProjectCategoryResource {

    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryProjectCategoryRepository classifications;

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        repositories.findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        return classifications.findForRepository(repositoryId)
                .stream().map(Entry::from).toList();
    }

    public record Entry(
            String categoryKey,
            String categoryName,
            String source,
            String confidence,
            Map<String, Object> rationale,
            OffsetDateTime observedAt
    ) {
        static Entry from(RepositoryProjectCategory c) {
            return new Entry(
                    c.getCategory().getCategoryKey(),
                    c.getCategory().getDisplayName(),
                    c.getSource().name(),
                    c.getConfidence().name(),
                    c.getRationale(),
                    c.getObservedAt()
            );
        }
    }
}
