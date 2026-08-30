package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    @Inject
    CurrentUserService currentUserService;

    @GET
    public Response me(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", current.user().getId());
        response.put("provider", current.identity().getProvider());
        response.put("login", current.identity().getLogin());
        response.put("displayName",
                current.identity().getDisplayName() == null ? "" : current.identity().getDisplayName());

        return Response.ok(response).build();
    }
}
