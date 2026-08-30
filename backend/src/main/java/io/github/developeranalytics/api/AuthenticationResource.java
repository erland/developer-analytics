package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;
import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationResource {
    @Inject AuthenticationService auth;
    @ConfigProperty(name="developer-analytics.frontend-url", defaultValue="/") String frontendUrl;

    @GET @Path("/github/login")
    public Response login(){
        return Response.seeOther(auth.startGitHubLogin()).build();
    }


@GET
@Path("/github/callback")
public Response callback(
        @QueryParam("code") String code,
        @QueryParam("state") String state,
        @CookieParam(AuthenticationService.SESSION_COOKIE) String token
) throws Exception {
    if (code == null || state == null) {
        throw new BadRequestException(
                "Missing GitHub OAuth callback parameters"
        );
    }

    AuthenticationService.CallbackResult result =
            auth.finishGitHubCallback(code, state, token);

    if (result.privateRepositoryAccessAuthorised()) {
        return Response.seeOther(
                URI.create(
                        frontendUrl +
                        "?private-repositories=authorised"
                )
        ).build();
    }

    return Response.seeOther(URI.create(frontendUrl))
            .header(
                    HttpHeaders.SET_COOKIE,
                    auth.sessionCookie(
                            result.token(),
                            result.expiresAt()
                    )
            )
            .build();
}


@GET
@Path("/github/private-repositories/authorise")
public Response authorisePrivateRepositories(
        @CookieParam(AuthenticationService.SESSION_COOKIE) String token
) {
    return Response.seeOther(
            auth.startGitHubPrivateRepositoryAuthorisation(token)
    ).build();
}

@POST
@Path("/github/private-repositories/remove")
public Response removePrivateRepositoryAccess(
        @CookieParam(AuthenticationService.SESSION_COOKIE) String token
) {
    auth.removeGitHubPrivateRepositoryAuthorisation(token);
    return Response.noContent().build();
}

    @GET @Path("/session")
    public Response session(@CookieParam(AuthenticationService.SESSION_COOKIE) String token){
        return auth.currentIdentity(token)
            .<Response>map(i -> Response.ok(Map.of(
                "authenticated",true,
                "provider","github",
                "login",i.getLogin(),
                "displayName",i.getDisplayName()==null?"":i.getDisplayName()
            )).build())
            .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("authenticated",false)).build());
    }

    @POST @Path("/logout")
    public Response logout(@CookieParam(AuthenticationService.SESSION_COOKIE) String token){
        auth.logout(token);
        return Response.noContent().header(HttpHeaders.SET_COOKIE,auth.clearSessionCookie()).build();
    }
}
