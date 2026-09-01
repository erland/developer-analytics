package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import io.github.developeranalytics.service.sync.ContributionSyncOrchestrator;
import io.github.developeranalytics.service.sync.RepositoryAnalysisOrchestrator;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/me/sync")
@Produces(MediaType.APPLICATION_JSON)
public class MeSyncResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    RepositoryDiscoveryJobService discoveryJobs;

    @Inject
    ContributionSyncOrchestrator contributionSync;

    @Inject
    RepositoryAnalysisOrchestrator repositoryAnalysis;

    @POST
    @Path("/github/contributions")
    public Response queueGitHubContributionDiscoveryBatch(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @jakarta.ws.rs.QueryParam("offset") @jakarta.ws.rs.DefaultValue("0") int offset,
            @jakarta.ws.rs.QueryParam("batchSize") @jakarta.ws.rs.DefaultValue("25") int batchSize
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        var result = contributionSync.enqueueBatch(
                current.user(),
                offset,
                batchSize
        );

        return Response.accepted(Map.of(
                "repositoriesConsidered", result.repositoriesConsidered(),
                "jobsQueued", result.jobsQueued(),
                "alreadyQueued", result.alreadyQueued(),
                "nextOffset", result.nextOffset() == null ? -1 : result.nextOffset()
        )).build();
    }


@POST
@Path("/github/repositories/{repositoryId}/language-evidence")
public Response queueGitHubLanguageEvidence(
        @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
        @PathParam("repositoryId") java.util.UUID repositoryId
) {
    CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
    BackgroundJob job =
            discoveryJobs.enqueueLanguageEvidence(current.user(), repositoryId);

    if (job == null) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of(
                        "status", "ALREADY_QUEUED",
                        "repositoryId", repositoryId
                ))
                .build();
    }

    return Response.accepted(Map.of(
            "jobId", job.getId(),
            "jobType", job.getJobType(),
            "status", job.getStatus().name()
    )).build();
}

    @POST
    @Path("/github/repositories/{repositoryId}/contributions")
    public Response queueGitHubContributionDiscovery(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") java.util.UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        BackgroundJob job = discoveryJobs.enqueueContributionDiscovery(current.user(), repositoryId);

        return Response.accepted(Map.of(
                "jobId", job.getId(),
                "jobType", job.getJobType(),
                "status", job.getStatus().name()
        )).build();
    }


    @POST
    @Path("/github/repositories/{repositoryId}/refresh-analysis")
    public Response refreshRepositoryAnalysis(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") java.util.UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        var result = repositoryAnalysis.enqueueRepository(current.user(), repositoryId);

        return Response.accepted(Map.of(
                "repositoryId", repositoryId,
                "repositoryJobsQueued", result.repositoryJobsQueued(),
                "alreadyQueued", result.alreadyQueued(),
                "aggregateJobsQueued", result.aggregateJobsQueued()
        )).build();
    }

    @POST
    @Path("/github/repositories")
    public Response queueGitHubRepositoryDiscovery(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        BackgroundJob job = discoveryJobs.enqueueGitHubDiscovery(current.user());

        return Response.accepted(Map.of(
                "jobId", job.getId(),
                "jobType", job.getJobType(),
                "status", job.getStatus().name()
        )).build();
    }
}
