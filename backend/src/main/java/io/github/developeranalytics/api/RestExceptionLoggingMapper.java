package io.github.developeranalytics.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Last-resort logging for unhandled REST failures.
 *
 * <p>The response deliberately contains no exception details. Server logs retain
 * the request path, exception type, root-cause type/message and full stack trace.
 * Correlation ids continue to be supplied by the existing MDC logging filter.</p>
 */
@Provider
@Priority(Priorities.USER + 500)
public class RestExceptionLoggingMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(RestExceptionLoggingMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webException) {
            Response response = webException.getResponse();
            if (response != null && response.getStatus() < 500) {
                return response;
            }
        }

        Throwable root = rootCause(exception);
        String path = uriInfo == null || uriInfo.getRequestUri() == null
                ? "unknown"
                : uriInfo.getRequestUri().getPath();

        LOG.errorf(
                exception,
                "event=rest_request_failed path=%s errorType=%s errorMessage=%s rootErrorType=%s rootErrorMessage=%s",
                sanitize(path),
                exception.getClass().getSimpleName(),
                sanitize(exception.getMessage()),
                root.getClass().getSimpleName(),
                sanitize(root.getMessage())
        );

        if (exception instanceof WebApplicationException webException
                && webException.getResponse() != null) {
            return webException.getResponse();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse("Internal server error"))
                .build();
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        int depth = 0;
        while (current.getCause() != null && current.getCause() != current && depth++ < 20) {
            current = current.getCause();
        }
        return current;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace('\n', ' ').replace('\r', ' ').replace('"', '\'');
    }

    public record ErrorResponse(String error) {}
}
