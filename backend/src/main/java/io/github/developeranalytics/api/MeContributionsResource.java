package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.*;
import io.github.developeranalytics.domain.model.Contribution;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.*;

@Path("/api/me/contributions")
@Produces(MediaType.APPLICATION_JSON)
public class MeContributionsResource {
    @Inject CurrentUserService currentUserService;
    @Inject EntityManager entityManager;

    @GET
    public Summary list(@CookieParam(AuthenticationService.SESSION_COOKIE) String token,
                        @QueryParam("limit") @DefaultValue("100") int limit) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        UUID userId = current.user().getId();
        List<Object[]> counts = entityManager.createQuery(
                "select c.type, count(c.id) from Contribution c where c.user.id=:userId " +
                "and c.repository.includedInAnalysis=true group by c.type", Object[].class)
                .setParameter("userId", userId).getResultList();
        EnumMap<Contribution.Type, Long> byType = new EnumMap<>(Contribution.Type.class);
        for (Object[] row : counts) byType.put((Contribution.Type) row[0], ((Number) row[1]).longValue());
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<Recent> recent = entityManager.createQuery(
                "select c.id, c.repository.id, c.repository.name, c.type, c.title, c.occurredAt " +
                "from Contribution c where c.user.id=:userId and c.repository.includedInAnalysis=true " +
                "order by c.occurredAt desc", Object[].class)
                .setParameter("userId", userId).setMaxResults(safeLimit).getResultList().stream()
                .map(r -> new Recent((UUID) r[0], (UUID) r[1], (String) r[2], ((Contribution.Type)r[3]).name(),
                        (String) r[4], (OffsetDateTime) r[5])).toList();
        long commits=byType.getOrDefault(Contribution.Type.COMMIT,0L), prs=byType.getOrDefault(Contribution.Type.PULL_REQUEST,0L),
                reviews=byType.getOrDefault(Contribution.Type.REVIEW,0L), issues=byType.getOrDefault(Contribution.Type.ISSUE,0L);
        return new Summary(commits+prs+reviews+issues, commits, prs, reviews, issues, recent);
    }
    public record Summary(long total,long commits,long pullRequests,long reviews,long issues,List<Recent> recent){}
    public record Recent(UUID id,UUID repositoryId,String repositoryName,String type,String title,OffsetDateTime occurredAt){}
}
