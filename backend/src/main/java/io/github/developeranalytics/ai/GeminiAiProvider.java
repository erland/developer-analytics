package io.github.developeranalytics.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

public class GeminiAiProvider implements AiProvider {

    private static final Logger LOG =
            Logger.getLogger(GeminiAiProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAiProvider(
            String apiKey,
            String model,
            String baseUrl,
            ObjectMapper objectMapper
    ) {
        this(
                apiKey,
                model,
                baseUrl,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
    }

    GeminiAiProvider(
            String apiKey,
            String model,
            String baseUrl,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.apiKey = Objects.requireNonNull(apiKey);
        this.model = Objects.requireNonNull(model);
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public String providerId() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String modelId() {
        return model;
    }

    @Override
    public Optional<ProjectClassificationResult> classifyProject(
            ProjectClassificationRequest request
    ) {
        return structured(
                "classifyProject",
                "Classify the project into concise project categories. "
                        + "Use only evidence in the supplied JSON. Input: "
                        + toJson(request),
                schema(
                        property(
                                "categories",
                                arrayOfStrings()
                        ),
                        property(
                                "confidence",
                                numberSchema(0, 1)
                        ),
                        property(
                                "rationale",
                                stringSchema()
                        ),
                        required("categories", "confidence", "rationale")
                ),
                node -> new ProjectClassificationResult(
                        stringList(node.path("categories")),
                        node.path("confidence").asDouble(0),
                        node.path("rationale").asText("")
                )
        );
    }

    @Override
    public Optional<ProjectSummaryResult> summariseProject(
            ProjectSummaryRequest request
    ) {
        return structured(
                "summariseProject",
                "Summarise the project based only on the supplied observed "
                        + "metadata and signals. Input: "
                        + toJson(request),
                schema(
                        property("summary", stringSchema()),
                        required("summary")
                ),
                node -> new ProjectSummaryResult(
                        node.path("summary").asText("")
                )
        );
    }

    @Override
    public Optional<TechnologyNormalisationResult> normaliseTechnologies(
            TechnologyNormalisationRequest request
    ) {
        Map<String, Object> itemSchema = schema(
                property("observedName", stringSchema()),
                property("canonicalName", stringSchema()),
                required("observedName", "canonicalName")
        );

        return structured(
                "normaliseTechnologies",
                "Normalise the supplied observed technology names to common "
                        + "canonical names. Do not add technologies that were "
                        + "not supplied. Input: "
                        + toJson(request),
                schema(
                        property(
                                "technologies",
                                arraySchema(itemSchema)
                        ),
                        required("technologies")
                ),
                node -> {
                    List<NormalisedTechnology> technologies =
                            new ArrayList<>();
                    for (JsonNode item :
                            node.path("technologies")) {
                        technologies.add(
                                new NormalisedTechnology(
                                        item.path("observedName")
                                                .asText(""),
                                        item.path("canonicalName")
                                                .asText("")
                                )
                        );
                    }
                    return new TechnologyNormalisationResult(
                            List.copyOf(technologies)
                    );
                }
        );
    }

    @Override
    public Optional<RoleInferenceResult> inferRoles(
            RoleInferenceRequest request
    ) {
        Map<String, Object> roleSchema = schema(
                property("role", stringSchema()),
                property("confidence", numberSchema(0, 1)),
                property("rationale", stringSchema()),
                required("role", "confidence", "rationale")
        );

        return structured(
                "inferRoles",
                "Infer likely development roles from the supplied project, "
                        + "technology and contribution signals. Treat roles as "
                        + "inferences, not facts. Input: "
                        + toJson(request),
                schema(
                        property("roles", arraySchema(roleSchema)),
                        required("roles")
                ),
                node -> {
                    List<InferredRole> roles = new ArrayList<>();
                    for (JsonNode item : node.path("roles")) {
                        roles.add(new InferredRole(
                                item.path("role").asText(""),
                                item.path("confidence").asDouble(0),
                                item.path("rationale").asText("")
                        ));
                    }
                    return new RoleInferenceResult(
                            List.copyOf(roles)
                    );
                }
        );
    }


@Override
public Optional<UserInsightsResult> summariseUserInsights(
        UserInsightsRequest request
) {
    Map<String, Object> roleSchema = schema(
            property("role", stringSchema()),
            property("confidence", numberSchema(0, 1)),
            property("rationale", stringSchema()),
            required("role", "confidence", "rationale")
    );

    return structured(
            "summariseUserInsights",
            "Generate cautious user-level developer insights using only "
                    + "the supplied aggregate repository, technology, "
                    + "project-category and contribution signals. "
                    + "Do not claim formal proficiency, employment title "
                    + "or facts not supported by the input. Input: "
                    + toJson(request),
            schema(
                    property("likelyRoles", arraySchema(roleSchema)),
                    property("technicalFocus", stringSchema()),
                    property("breadthDepthObservation", stringSchema()),
                    property("technologyEvolutionSummary", stringSchema()),
                    property("openSourceEngagementSummary", stringSchema()),
                    required(
                            "likelyRoles",
                            "technicalFocus",
                            "breadthDepthObservation",
                            "technologyEvolutionSummary",
                            "openSourceEngagementSummary"
                    )
            ),
            node -> {
                List<InferredRole> roles = new ArrayList<>();
                for (JsonNode item : node.path("likelyRoles")) {
                    roles.add(new InferredRole(
                            item.path("role").asText(""),
                            item.path("confidence").asDouble(0),
                            item.path("rationale").asText("")
                    ));
                }

                return new UserInsightsResult(
                        List.copyOf(roles),
                        node.path("technicalFocus").asText(""),
                        node.path("breadthDepthObservation").asText(""),
                        node.path("technologyEvolutionSummary").asText(""),
                        node.path("openSourceEngagementSummary").asText("")
                );
            }
    );
}

    @Override
    public Optional<TechnologyHistorySummaryResult>
    summariseTechnologyHistory(
            TechnologyHistorySummaryRequest request
    ) {
        return structured(
                "summariseTechnologyHistory",
                "Summarise how use of the technology changes over time using "
                        + "only the supplied aggregate history. Input: "
                        + toJson(request),
                schema(
                        property("summary", stringSchema()),
                        required("summary")
                ),
                node -> new TechnologyHistorySummaryResult(
                        node.path("summary").asText("")
                )
        );
    }

    private <T> Optional<T> structured(
            String requestType,
            String prompt,
            Map<String, Object> responseSchema,
            Function<JsonNode, T> mapper
    ) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents",
                    List.of(Map.of(
                            "role",
                            "user",
                            "parts",
                            List.of(Map.of("text", prompt))
                    )),
                    "generationConfig",
                    Map.of(
                            "temperature",
                            0.1,
                            "responseFormat",
                            Map.of(
                                    "text",
                                    Map.of(
                                            "mimeType",
                                            "application/json",
                                            "schema",
                                            responseSchema
                                    )
                            )
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint())
                    .timeout(Duration.ofSeconds(45))
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(
                                    requestBody
                            ),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                LOG.warnf(
                        "AI request failed provider=gemini "
                                + "type=%s status=%d",
                        requestType,
                        response.statusCode()
                );
                return Optional.empty();
            }

            JsonNode responseJson =
                    objectMapper.readTree(response.body());
            JsonNode usage = responseJson.path(
                    "usageMetadata"
            );

            LOG.infof(
                    "AI request succeeded provider=gemini "
                            + "type=%s promptTokens=%s "
                            + "candidateTokens=%s totalTokens=%s",
                    requestType,
                    tokenValue(
                            usage,
                            "promptTokenCount"
                    ),
                    tokenValue(
                            usage,
                            "candidatesTokenCount"
                    ),
                    tokenValue(
                            usage,
                            "totalTokenCount"
                    )
            );

            String structuredText = responseJson
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText(null);

            if (structuredText == null ||
                    structuredText.isBlank()) {
                LOG.warnf(
                        "AI request failed provider=gemini "
                                + "type=%s reason=empty-response",
                        requestType
                );
                return Optional.empty();
            }

            JsonNode structuredJson =
                    objectMapper.readTree(
                            structuredText
                    );

            return Optional.ofNullable(
                    mapper.apply(structuredJson)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warnf(
                    "AI request interrupted provider=gemini "
                            + "type=%s",
                    requestType
            );
            return Optional.empty();
        } catch (Exception exception) {
            // Never log prompts or request bodies here.
            LOG.warnf(
                    "AI request failed provider=gemini "
                            + "type=%s exception=%s",
                    requestType,
                    exception.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    private URI endpoint() {
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(
                        0,
                        baseUrl.length() - 1
                )
                : baseUrl;
        return URI.create(
                normalized
                        + "/models/"
                        + model
                        + ":generateContent"
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Unable to serialize AI input",
                    exception
            );
        }
    }

    private String tokenValue(
            JsonNode usage,
            String field
    ) {
        return usage.has(field)
                ? usage.get(field).asText()
                : "unknown";
    }

    private List<String> stringList(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }

    private static Map.Entry<String, Object> property(
            String name,
            Object schema
    ) {
        return Map.entry(name, schema);
    }

    @SafeVarargs
    private static Map<String, Object> schema(
            Map.Entry<String, Object>... entries
    ) {
        Map<String, Object> properties =
                new LinkedHashMap<>();
        List<String> required =
                new ArrayList<>();

        for (Map.Entry<String, Object> entry :
                entries) {
            if ("__required".equals(entry.getKey())) {
                @SuppressWarnings("unchecked")
                List<String> requiredNames =
                        (List<String>) entry.getValue();
                required.addAll(requiredNames);
            } else {
                properties.put(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        Map<String, Object> schema =
                new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put(
                "additionalProperties",
                false
        );
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map.Entry<String, Object> required(
            String... names
    ) {
        return Map.entry(
                "__required",
                List.of(names)
        );
    }

    private static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> numberSchema(
            double minimum,
            double maximum
    ) {
        return Map.of(
                "type", "number",
                "minimum", minimum,
                "maximum", maximum
        );
    }

    private static Map<String, Object> arrayOfStrings() {
        return arraySchema(stringSchema());
    }

    private static Map<String, Object> arraySchema(
            Map<String, Object> items
    ) {
        return Map.of(
                "type", "array",
                "items", items
        );
    }
}
