package com.proactiveos.extraction;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiCompatibleExtractionService implements AiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleExtractionService.class);
    private static final Pattern BEARER_VALUE = Pattern.compile("(?i)(Bearer\\s+)[^\\s\\\"']+");
    private static final Pattern URL_CREDENTIALS = Pattern.compile("(?i)(https?://)[^/@]+@", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHORIZATION_VALUE = Pattern.compile("(?im)(authorization\\s*[:=]\\s*)([^\\r\\n]+)");
    private static final Set<Integer> TRANSIENT_STATUS_CODES = Set.of(429, 500, 502, 503, 504);
    private static final int MAX_ATTEMPTS = 3;

    private final AiProviderProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleExtractionService(AiProviderProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String extract(String journalContent, LocalDate entryDate, ZoneId timeZone) {
        if (isBlank(properties.baseUrl()) || isBlank(properties.model()) || isBlank(properties.apiKey())) {
            throw new AiExtractionException("AI extraction is not configured. Set AI_BASE_URL, AI_MODEL, and AI_API_KEY.");
        }

        boolean apiKeyPresent = !isBlank(properties.apiKey());
        String requestUrl = "<not constructed>";
        String stage = "URI construction";
        try {
            stage = "Authorization token validation";
            String bearerToken = normalizedBearerToken(properties.apiKey());

            requestUrl = endpointUrl(properties.baseUrl());
            log.info("AI extraction request: url={}, model={}, apiKeyPresent={}",
                    safeUrl(requestUrl), properties.model(), apiKeyPresent);

            stage = "request prompt construction";
            String systemPrompt = """
                You extract only factual life events explicitly stated in journal text.
                Return ONLY the JSON object matching the schema below. Do not explain your reasoning.
                Do not include Markdown fences or commentary before or after the JSON. Keep the output minimal.
                The top-level object must contain only the "events" property. Every event object must include all 17
                properties declared in the schema. Use null for unknown values and for every property not applicable
                to the selected event type. Never omit required schema properties and never invent values.
                Extract only these event types and event-specific fields:
                SLEEP: durationMinutes, notes. eventTime is only a clearly stated sleep onset (went to bed/fell asleep).
                Put explicitly stated wake times in notes; if only waking is stated, leave eventTime null. Never infer sleep duration from separate sleep and wake times.
                WATER: quantity, unit.
                FOOD: description, calories.
                STUDY: subject, durationMinutes.
                WORKOUT: activityType, durationMinutes. Do not use description or notes for workouts.
                STEPS: count.
                JOB_APPLICATION: company, role, status, applicationCount.
                MOOD: mood, notes.
                Only fields listed for the selected type may be non-null. All other event-specific fields MUST be null;
                do not populate them merely because they are available in the shared schema. eventTime and confidence
                are shared fields and may be non-null only when directly supported by the journal.
                eventTime represents the event's start/activity time. Extract an independent time for each event. When the
                text gives a clock time without an offset, resolve its full local date using the journal date and the
                supplied time zone, then return YYYY-MM-DDTHH:mm:ss with no offset. The server converts that wall time
                to UTC. Preserve clock precision: "around 8 PM" may be represented as 20:00:00; never add unsupported
                minutes or seconds. Support 12-hour AM/PM and 24-hour clocks. For a range, eventTime is the start.
                Resolve explicit relative dates (today, yesterday, last night) only when context identifies one date.
                A vague period alone ("this morning", "in the morning", "this afternoon", "in the evening", "later", "sometime today")
                is not a clock time: set eventTime null. If date or time remains ambiguous, set eventTime null.
                For durationMinutes, convert explicit durations such as "40 minutes" to 40 and sensible approximations
                such as "about 2 hours" to minutes (120); convert decimal hours exactly (1.5 hours = 90). Derive a duration from explicit start
                and end times only when both endpoints and the elapsed interval are unambiguous. Inequalities/lower bounds
                such as ">30 mins" are not exact durations: set durationMinutes null. Never fabricate precision.
                {\"events\":[{\"type\":\"SLEEP|WATER|FOOD|STUDY|WORKOUT|STEPS|JOB_APPLICATION|MOOD\",\"eventTime\":\"journal-local ISO-8601 date-time YYYY-MM-DDTHH:mm:ss or null\",\"confidence\":0.0,\"subject\":null,\"durationMinutes\":null,\"quantity\":null,\"unit\":null,\"description\":null,\"calories\":null,\"activityType\":null,\"count\":null,\"company\":null,\"role\":null,\"status\":null,\"mood\":null,\"notes\":null,\"applicationCount\":null}]}
                Do not return source or journalEntryId. Never advise, diagnose, infer unsupported values, or add defaults.
                Resolve relative dates/times only from the supplied journal date and time zone.
                If a clock time/date cannot be resolved reliably, set eventTime to null. A walk alone does not imply
                duration or steps. Map application counts to applicationCount and step counts to count.
                """;
                String userPrompt = "Journal entry date: " + entryDate + "\nTime zone: " + timeZone
                    + "\nJournal text:\n" + journalContent;

                stage = "request body construction";
                Map<String, Object> payload = Map.of(
                    "model", properties.model(),
                    "temperature", 0,
                    "reasoning_effort", "low",
                    "include_reasoning", false,
                    "max_completion_tokens", 2048,
                        "response_format", Map.of(
                            "type", "json_schema",
                            "json_schema", Map.of(
                                "name", "life_event_extraction",
                                "strict", true,
                                "schema", extractionSchema())),
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                    )
                );
                    log.info("AI extraction request body summary: {}", summarizePayload(payload));

                stage = "request serialization and HTTP exchange";
                Map<?, ?> response = executeWithRetry(requestUrl, payload, bearerToken, apiKeyPresent);
                stage = "response parsing";
            if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
                throw new AiExtractionException("AI provider returned an empty extraction response.");
            }
            Object first = choices.getFirst();
            if (!(first instanceof Map<?, ?> choice)
                    || !(choice.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)) {
                throw new AiExtractionException("AI provider returned an invalid structured response.");
            }
            return content;
        } catch (RestClientResponseException exception) {
            String responseBody = safeText(exception.getResponseBodyAsString());
            log.error("AI provider returned HTTP {} for url={}, model={}, apiKeyPresent={}, body={}",
                    exception.getStatusCode().value(), safeUrl(requestUrl), properties.model(), apiKeyPresent,
                    truncate(responseBody));
            throw new AiExtractionException("AI provider returned HTTP " + exception.getStatusCode().value()
                    + ": " + truncate(responseBody), exception);
        } catch (RestClientException exception) {
            String reason = safeText(exception.getMessage());
            log.error("AI provider transport failure for url={}, model={}, apiKeyPresent={}, errorType={}, detail={}",
                    safeUrl(requestUrl), properties.model(), apiKeyPresent, exception.getClass().getSimpleName(), reason);
            throw new AiExtractionException("AI provider request failed: " + reason, exception);
            } catch (IllegalArgumentException exception) {
                String message = safeText(exception.getMessage(), journalContent);
                String stackTrace = safeStackTrace(exception, journalContent);
                log.error("AI provider IllegalArgumentException: stage={}, url={}, model={}, apiKeyPresent={}, message={}, fullStackTrace={}",
                    stage, safeUrl(requestUrl), properties.model(), apiKeyPresent, message, stackTrace);
                throw exception;
        }
    }

    private Map<?, ?> executeWithRetry(String requestUrl, Map<String, Object> payload,
                                       String bearerToken, boolean apiKeyPresent) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                RestClient.RequestBodySpec request = restClient.post()
                        .uri(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON);
                request.headers(headers -> headers.setBearerAuth(bearerToken));
                return request.body(payload).retrieve().body(Map.class);
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                String responseBody = truncate(safeText(exception.getResponseBodyAsString()));
                if (!TRANSIENT_STATUS_CODES.contains(status)) {
                    log.error("AI provider returned non-retryable HTTP {} for url={}, model={}, apiKeyPresent={}, body={}",
                            status, safeUrl(requestUrl), properties.model(), apiKeyPresent, responseBody);
                    throw new AiExtractionException("AI provider returned HTTP " + status + ": " + responseBody, exception);
                }

                if (attempt == MAX_ATTEMPTS) {
                    log.error("AI provider remained unavailable after {} attempts: HTTP {} for url={}, model={}, apiKeyPresent={}, body={}",
                            MAX_ATTEMPTS, status, safeUrl(requestUrl), properties.model(), apiKeyPresent, responseBody);
                    throw new AiExtractionException(
                            "AI provider is temporarily unavailable after " + MAX_ATTEMPTS
                                    + " attempts (HTTP " + status + "). Please try again later.", exception);
                }

                long delayMillis = 1000L << (attempt - 1);
                log.warn("Transient AI provider HTTP {} on attempt {}/{} for url={}, model={}, apiKeyPresent={}; retrying in {} ms",
                        status, attempt, MAX_ATTEMPTS, safeUrl(requestUrl), properties.model(), apiKeyPresent, delayMillis);
                sleepBeforeRetry(delayMillis);
            }
        }
        throw new IllegalStateException("Retry loop exited unexpectedly.");
    }

    private void sleepBeforeRetry(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiExtractionException("AI provider retry was interrupted.", exception);
        }
    }

    private String endpointUrl(String baseUrl) {
        String normalizedBaseUrl = baseUrl.replaceAll("/+$", "");
        String suffix = "/chat/completions";
        if (normalizedBaseUrl.endsWith(suffix)) {
            return normalizedBaseUrl;
        }
        return normalizedBaseUrl + suffix;
    }

    private String normalizedBearerToken(String apiKey) {
        String token = apiKey.trim();
        if (token.chars().anyMatch(character -> Character.isISOControl(character) || Character.isWhitespace(character))) {
            throw new AiExtractionException("AI_API_KEY contains invalid whitespace or control characters.");
        }
        return token;
    }

        private Map<String, Object> summarizePayload(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseFormat = (Map<String, Object>) payload.get("response_format");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");
        List<Map<String, Object>> messageSummaries = messages.stream()
            .map(message -> Map.<String, Object>of(
                "role", message.get("role"),
                "contentCharacters", message.get("content").length()))
            .toList();
        return Map.of(
            "model", payload.get("model"),
            "temperature", payload.get("temperature"),
            "reasoning_effort", payload.get("reasoning_effort"),
            "include_reasoning", payload.get("include_reasoning"),
            "max_completion_tokens", payload.get("max_completion_tokens"),
            "response_format", responseFormat,
            "messages", messageSummaries);
        }

    private String safeUrl(String url) {
        String withoutUserInfo = URL_CREDENTIALS.matcher(url).replaceAll("$1[REDACTED]@");
        int queryIndex = withoutUserInfo.indexOf('?');
        String withoutQuery = queryIndex >= 0 ? withoutUserInfo.substring(0, queryIndex) + "?[REDACTED]" : withoutUserInfo;
        return safeText(withoutQuery, null);
    }

        private Map<String, Object> extractionSchema() {
            Map<String, Object> eventProperties = new LinkedHashMap<>();
            eventProperties.put("type", Map.of("type", "string", "enum",
                    List.of("SLEEP", "WATER", "FOOD", "STUDY", "WORKOUT", "STEPS", "JOB_APPLICATION", "MOOD")));
            eventProperties.put("eventTime", nullableType("string"));
            eventProperties.put("confidence", nullableType("number"));
            eventProperties.put("subject", nullableType("string"));
            eventProperties.put("durationMinutes", nullableType("integer"));
            eventProperties.put("quantity", nullableType("integer"));
            eventProperties.put("unit", nullableEnum(List.of("GLASS", "ML", "LITER")));
            eventProperties.put("description", nullableType("string"));
            eventProperties.put("calories", nullableType("integer"));
            eventProperties.put("activityType", nullableType("string"));
            eventProperties.put("count", nullableType("integer"));
            eventProperties.put("company", nullableType("string"));
            eventProperties.put("role", nullableType("string"));
            eventProperties.put("status", nullableType("string"));
            eventProperties.put("mood", nullableType("string"));
            eventProperties.put("notes", nullableType("string"));
            eventProperties.put("applicationCount", nullableType("integer"));

            Map<String, Object> eventItem = Map.of(
                    "type", "object",
                    "properties", eventProperties,
                    "required", List.copyOf(eventProperties.keySet()),
                    "additionalProperties", false);
        Map<String, Object> eventsProperty = Map.of(
            "type", "array",
                "items", eventItem);
        return Map.of(
            "type", "object",
            "properties", Map.of("events", eventsProperty),
            "required", List.of("events"),
            "additionalProperties", false);
        }

        private Map<String, Object> nullableType(String type) {
            return Map.of("type", List.of(type, "null"));
        }

        private Map<String, Object> nullableEnum(List<String> values) {
        List<Object> enumValues = new ArrayList<>(values);
        enumValues.add(null);
        return Map.of("type", List.of("string", "null"), "enum", enumValues);
        }

    private String safeText(String value) {
        return safeText(value, null);
    }

    private String safeText(String value, String journalContent) {
        if (value == null) {
            return "";
        }
        String withoutKey = isBlank(properties.apiKey()) ? value : value.replace(properties.apiKey(), "[REDACTED]");
        String withoutBearer = BEARER_VALUE.matcher(withoutKey).replaceAll("$1[REDACTED]");
        String withoutAuthorization = AUTHORIZATION_VALUE.matcher(withoutBearer).replaceAll("$1[REDACTED]");
        return journalContent == null || journalContent.isEmpty()
                ? withoutAuthorization
                : withoutAuthorization.replace(journalContent, "[JOURNAL_TEXT_REDACTED]");
    }

    private String safeStackTrace(Throwable exception, String journalContent) {
        StringWriter buffer = new StringWriter();
        exception.printStackTrace(new PrintWriter(buffer));
        return safeText(buffer.toString(), journalContent);
    }

    private String truncate(String value) {
        int maxLength = 4000;
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...[truncated]";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
