package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.account.UserDataDeletionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/api/me/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeDataDeletionResource {

    public static final String CONFIRMATION = "DELETE_MY_DATA";

    @Inject
    CurrentUserService currentUserService;

    @Inject
    UserDataDeletionService deletionService;

    @DELETE
    public Response deleteAll(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            DeleteRequest request
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        if (request == null ||
                !CONFIRMATION.equals(request.confirmation())) {
            throw new BadRequestException(
                    "Explicit DELETE_MY_DATA confirmation is required"
            );
        }

        UserDataDeletionService.DeletionResult result =
                deletionService.deleteUser(current.user().getId());

        NewCookie expiredSession = new NewCookie.Builder(
                AuthenticationService.SESSION_COOKIE
        )
        .value("")
        .path("/")
        .maxAge(0)
        .httpOnly(true)
        .secure(false)
        .build();

        return Response.ok(new DeleteResponse(
                        true,
                        result.deletedDataCounts(),
                        result.persistedReportsDeleted(),
                        "Generated Markdown/PDF reports are not persisted server-side."
                ))
                .cookie(expiredSession)
                .build();
    }

    public record DeleteRequest(String confirmation) {}

    public record DeleteResponse(
            boolean deleted,
            java.util.Map<String, Long> deletedDataCounts,
            int persistedReportsDeleted,
            String reportDeletionNote
    ) {}
}
