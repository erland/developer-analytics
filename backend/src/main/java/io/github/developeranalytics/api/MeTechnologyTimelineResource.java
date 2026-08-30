package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.technology.TechnologyTimelineService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/api/me/technology-timeline")
@Produces(MediaType.APPLICATION_JSON)
public class MeTechnologyTimelineResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    TechnologyTimelineService timelines;

    @POST
    @Path("/recalculate")
    public Map<String, Object> recalculate(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        int monthsUpdated = timelines.recalculate(current.user());

        return Map.of(
                "status", "COMPLETED",
                "monthsUpdated", monthsUpdated
        );
    }

    @GET
    public List<TechnologyTimelineService.TechnologyTimeline> get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        return timelines.build(current.user());
    }
}
