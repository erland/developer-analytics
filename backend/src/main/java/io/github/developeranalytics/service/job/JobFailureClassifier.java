package io.github.developeranalytics.service.job;

import io.github.developeranalytics.provider.ProviderException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobFailureClassifier {

    public Classification classify(Throwable failure) {
        Throwable current = failure;

        while (current != null) {
            if (current instanceof ProviderException providerFailure) {
                int status = providerFailure.getStatusCode();

                // Only an authentication failure is treated as global provider
                // access loss. A 403 may be repository-specific or a provider
                // throttle, and a 404 commonly means one repository disappeared.
                // Neither must poison every repository for the user.
                if (status == 401) {
                    return new Classification(
                            false,
                            true,
                            "Provider authentication lost"
                    );
                }

                if (status == 404) {
                    return new Classification(
                            false,
                            false,
                            "Provider resource no longer available"
                    );
                }

                if (status == 403 || status == 408 || status == 409 ||
                        status == 425 || status == 429 ||
                        status >= 500) {
                    return new Classification(
                            true,
                            false,
                            "Temporary provider error"
                    );
                }
            }

            if (current instanceof java.net.http.HttpTimeoutException ||
                    current instanceof java.net.ConnectException ||
                    current instanceof java.net.SocketTimeoutException ||
                    current instanceof java.io.IOException) {
                return new Classification(
                        true,
                        false,
                        "Temporary network/provider error"
                );
            }

            current = current.getCause();
        }

        return new Classification(
                true,
                false,
                "Retriable job failure"
        );
    }

    public record Classification(
            boolean retriable,
            boolean providerAccessLost,
            String reason
    ) {}
}
