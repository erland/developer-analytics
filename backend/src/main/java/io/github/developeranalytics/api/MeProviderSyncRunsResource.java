package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.sync.ProviderSyncRunService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/me/provider-sync-runs")
@Produces(MediaType.APPLICATION_JSON)
public class MeProviderSyncRunsResource {
    @Inject CurrentUserService currentUserService;
    @Inject ProviderSyncRunService syncRuns;

    @GET
    public List<ProviderSyncRunService.Summary> recent(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return syncRuns.recentForUser(current.user().getId()).stream().map(syncRuns::summarize).toList();
    }

    @GET
    @Path("/{id}")
    public ProviderSyncRunService.Summary get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("id") UUID id) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return syncRuns.findForUser(id, current.user().getId()).map(syncRuns::summarize).orElseThrow(NotFoundException::new);
    }
}
