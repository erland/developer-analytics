package io.github.developeranalytics.observability;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class CorrelationIdFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String supplied = requestContext.getHeaderString(HEADER);
        String correlationId = valid(supplied)
                ? supplied
                : UUID.randomUUID().toString();

        requestContext.setProperty(MDC_KEY, correlationId);
        MDC.put(MDC_KEY, correlationId);
    }

    @Override
    public void filter(
            ContainerRequestContext requestContext,
            ContainerResponseContext responseContext
    ) {
        Object value = requestContext.getProperty(MDC_KEY);
        if (value != null) {
            responseContext.getHeaders().putSingle(
                    HEADER,
                    value.toString()
            );
        }
        MDC.remove(MDC_KEY);
    }

    static boolean valid(String value) {
        return value != null
                && value.length() >= 8
                && value.length() <= 128
                && value.matches("[A-Za-z0-9._:-]+");
    }
}
