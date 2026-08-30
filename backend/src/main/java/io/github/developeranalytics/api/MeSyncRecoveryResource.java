package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import io.github.developeranalytics.service.sync.SynchronisationRecoveryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me/sync-recovery")
@Produces(MediaType.APPLICATION_JSON)
public class MeSyncRecoveryResource {

    @Inject CurrentUserService currentUserService;
    @Inject SynchronisationRecoveryService recovery;
    @Inject RepositoryDiscoveryJobService discoveryJobs;

    @POST
    @Path("/recover")
    public RecoveryResponse recover(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        currentUserService.requireCurrentUser(sessionToken);
        int recovered = recovery.recoverInterruptedJobs();
        return new RecoveryResponse(recovered);
    }

    @POST
    @Path("/github/retry")
    public RetryResponse retryGitHub(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        discoveryJobs.enqueueGitHubDiscovery(current.user());
        return new RetryResponse(true);
    }

    public record RecoveryResponse(int recoveredJobs) {}
    public record RetryResponse(boolean queued) {}
}
