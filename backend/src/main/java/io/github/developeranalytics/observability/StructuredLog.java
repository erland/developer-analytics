package io.github.developeranalytics.observability;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

public final class StructuredLog {

    private StructuredLog() {}

    public static void info(
            Logger log,
            String event,
            Map<String, ?> fields
    ) {
        log.info(format(event, fields));
    }

    public static void warn(
            Logger log,
            String event,
            Throwable failure,
            Map<String, ?> fields
    ) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (fields != null) safe.putAll(fields);
        safe.put("errorType", failure.getClass().getSimpleName());
        safe.put("errorMessage", sanitize(failure.getMessage()));
        log.warn(format(event, safe));
    }

    public static Map<String, Object> fields(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    public static String format(
            String event,
            Map<String, ?> fields
    ) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("event=" + token(event));

        Object correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            joiner.add("correlationId=" + token(correlationId));
        }

        if (fields != null) {
            fields.forEach((key, value) -> {
                if (isSensitiveKey(key)) return;
                if (value == null) return;
                joiner.add(token(key) + "=" + token(value));
            });
        }

        return joiner.toString();
    }

    static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase();
        return normalized.contains("token")
                || normalized.contains("credential")
                || normalized.contains("secret")
                || normalized.contains("authorization")
                || normalized.contains("sourcecontent")
                || normalized.contains("prompt")
                || normalized.contains("diff");
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) return "n/a";
        String compact = value.replaceAll("[\r\n\t]+", " ").trim();
        if (compact.length() > 240) {
            compact = compact.substring(0, 240);
        }
        // Provider exception messages can contain URLs but must never contain
        // access-token query values in structured logs.
        compact = compact.replaceAll(
                "(?i)(access_token|token|api_key|apikey|authorization)=([^&\\s]+)",
                "$1=[REDACTED]"
        );
        return compact;
    }

    private static String token(Object value) {
        String text = String.valueOf(value)
                .replaceAll("[\r\n\t ]+", "_");
        return text.length() > 300
                ? text.substring(0, 300)
                : text;
    }

    public static String id(UUID id) {
        return id == null ? null : id.toString();
    }
}
