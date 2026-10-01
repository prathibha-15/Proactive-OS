package com.proactiveos.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleExtractionServiceTest {

    private static final String API_KEY = "test-key-that-must-not-appear-in-errors";
    private static final LocalDate ENTRY_DATE = LocalDate.of(2026, 9, 28);

    @Test
    void postsToExpectedOpenAiCompatibleEndpointWithBearerAuthAndConfiguredModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-3.8-flash");
        String journalText = "A short journal.";
        Logger logger = (Logger) LoggerFactory.getLogger(OpenAiCompatibleExtractionService.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(content().string(validRequestBody(journalText)))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\"events\\":[]}"}}]}
                        """, MediaType.APPLICATION_JSON));

        String content = service.extract(journalText, ENTRY_DATE, ZoneId.of("Asia/Kolkata"));

        assertThat(content).isEqualTo("{\"events\":[]}");
        String logged = appender.list.stream().map(event -> event.getFormattedMessage()).reduce("", String::concat);
        assertThat(logged).contains("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
                .contains("model=gemini-3.8-flash")
                .contains("request body summary")
                .doesNotContain(API_KEY)
                .doesNotContain(journalText);
        server.verify();
        logger.detachAppender(appender);
    }

    @Test
    void doesNotAppendChatCompletionsTwiceWhenBaseAlreadyIncludesIt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions/", "gemini-3.8-flash");
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"events\\\":[]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata"));

        server.verify();
    }

    @Test
    void trimsSurroundingWhitespaceFromBearerTokenBeforeHeaderConstruction() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = new OpenAiCompatibleExtractionService(
                new AiProviderProperties("https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash",
                        "  " + API_KEY + "\r\n", "Asia/Kolkata"), builder);
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"events\\\":[]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata"));

        server.verify();
    }

    @Test
    void rejectsInternalBearerTokenControlCharactersWithoutEchoingTheToken() {
        RestClient.Builder builder = RestClient.builder();
        OpenAiCompatibleExtractionService service = new OpenAiCompatibleExtractionService(
                new AiProviderProperties("https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash",
                        API_KEY + "\nmalformed", "Asia/Kolkata"), builder);

        assertThatThrownBy(() -> service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("invalid whitespace or control characters")
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    void surfacesSanitizedProviderStatusAndBodyWithoutApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash");
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"model not found; token=" + API_KEY + "\"}}"));

        assertThatThrownBy(() -> service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageContaining("model not found")
                .hasMessageNotContaining(API_KEY);
        server.verify();
    }

    @Test
    void retries503ThenSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash");
        String url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("busy"));
        server.expect(requestTo(url)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"events\\\":[]}\"}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                .isEqualTo("{\"events\":[]}");
        server.verify();
    }

    @Test
    void stopsAfterThree503ResponsesWithClearUnavailableError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash");
        String url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        for (int attempt = 0; attempt < 3; attempt++) {
            server.expect(requestTo(url))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("temporarily overloaded"));
        }

        assertThatThrownBy(() -> service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("temporarily unavailable after 3 attempts (HTTP 503)");
        server.verify();
    }

    @Test
    void doesNotRetry400() {
        assertNonRetryableStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void doesNotRetry401() {
        assertNonRetryableStatus(HttpStatus.UNAUTHORIZED);
    }

    private void assertNonRetryableStatus(HttpStatus status) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleExtractionService service = service(builder,
                "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash");
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(withStatus(status).contentType(MediaType.TEXT_PLAIN).body("non-retryable"));

        assertThatThrownBy(() -> service.extract("A short journal.", ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("HTTP " + status.value())
                .hasMessageNotContaining("temporarily unavailable after");
        server.verify();
    }

        @Test
        void logsIllegalArgumentExceptionStageAndStackWithoutKeyOrJournalText() {
                String journalText = "private journal text must not appear";
                RestClient.Builder builder = RestClient.builder();
                OpenAiCompatibleExtractionService service = service(builder, "http://[invalid", "gemini-3.8-flash");
                Logger logger = (Logger) LoggerFactory.getLogger(OpenAiCompatibleExtractionService.class);
                ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
                appender.start();
                logger.addAppender(appender);
                logger.setLevel(Level.ERROR);

                try {
                        assertThatThrownBy(() -> service.extract(journalText, ENTRY_DATE, ZoneId.of("Asia/Kolkata")))
                                        .isInstanceOf(IllegalArgumentException.class);

                        String diagnostics = appender.list.stream()
                                        .map(event -> event.getFormattedMessage())
                                        .reduce("", (left, right) -> left + "\n" + right);
                        assertThat(diagnostics)
                                        .contains("IllegalArgumentException")
                                                        .contains("stage=request serialization and HTTP exchange")
                                        .contains("fullStackTrace=")
                                        .doesNotContain(API_KEY)
                                        .doesNotContain(journalText);
                        assertThat(appender.list).isNotEmpty();
                } finally {
                        logger.detachAppender(appender);
                }
        }

    private OpenAiCompatibleExtractionService service(RestClient.Builder builder, String baseUrl, String model) {
        return new OpenAiCompatibleExtractionService(
                new AiProviderProperties(baseUrl, model, API_KEY, "Asia/Kolkata"), builder);
    }

        private Matcher<String> validRequestBody(String journalText) {
                return new BaseMatcher<>() {
                        @Override
                        public boolean matches(Object item) {
                                try {
                                        JsonNode root = new ObjectMapper().readTree((String) item);
                                        Set<String> fields = new HashSet<>();
                                        root.fieldNames().forEachRemaining(fields::add);
                                        assertThat(fields).isEqualTo(Set.of("model", "temperature", "reasoning_effort",
                                                        "include_reasoning", "max_completion_tokens", "response_format", "messages"));
                                        assertThat(root.path("model").asText()).isEqualTo("gemini-3.8-flash");
                                        assertThat(root.path("temperature").asInt()).isZero();
                                        assertThat(root.path("reasoning_effort").asText()).isEqualTo("low");
                                        assertThat(root.path("include_reasoning").asBoolean()).isFalse();
                                        assertThat(root.path("max_completion_tokens").asInt()).isEqualTo(2048);
                                        JsonNode responseFormat = root.path("response_format");
                                        assertThat(responseFormat.path("type").asText()).isEqualTo("json_schema");
                                        JsonNode jsonSchema = responseFormat.path("json_schema");
                                        assertThat(jsonSchema.path("name").asText()).isEqualTo("life_event_extraction");
                                        assertThat(jsonSchema.path("strict").asBoolean()).isTrue();
                                        assertStrictEventSchema(jsonSchema.path("schema"));
                                        assertThat(root.path("messages").size()).isEqualTo(2);
                                        assertThat(root.path("messages").get(0).path("role").asText()).isEqualTo("system");
                                        assertThat(root.path("messages").get(0).path("content").asText())
                                                        .startsWith("You extract only factual life events explicitly stated in journal text.")
                                                        .contains("Return ONLY the JSON object")
                                                        .contains("Do not explain your reasoning")
                                                        .contains("Do not include Markdown fences")
                                                        .contains("top-level object must contain only the \"events\" property")
                                                        .contains("Every event object must include all 17")
                                                        .contains("Use null for unknown values")
                                                        .contains("for every property not applicable")
                                                        .contains("Only fields listed for the selected type may be non-null")
                                                        .contains("All other event-specific fields MUST be null")
                                                        .contains("SLEEP: durationMinutes, notes")
                                                        .contains("WATER: quantity, unit")
                                                        .contains("FOOD: description, calories")
                                                        .contains("STUDY: subject, durationMinutes")
                                                        .contains("WORKOUT: activityType, durationMinutes")
                                                        .contains("STEPS: count")
                                                        .contains("JOB_APPLICATION: company, role, status, applicationCount")
                                                        .contains("MOOD: mood, notes")
                                                        .contains("omit durationMinutes");
                                        assertThat(root.path("messages").get(1).path("role").asText()).isEqualTo("user");
                                        assertThat(root.path("messages").get(1).path("content").asText())
                                                        .isEqualTo("Journal entry date: 2026-09-28\nTime zone: Asia/Kolkata\nJournal text:\n" + journalText);
                                        return true;
                                } catch (Exception exception) {
                                        throw new AssertionError("Request body must be valid JSON with the expected provider shape.", exception);
                                }
                        }

                        @Override
                        public void describeTo(Description description) {
                                description.appendText("valid Gemini OpenAI-compatible JSON request body");
                        }
                };
        }

        private void assertStrictEventSchema(JsonNode schema) {
                Set<String> rootProperties = new HashSet<>();
                schema.path("properties").fieldNames().forEachRemaining(rootProperties::add);
                assertThat(schema.path("type").asText()).isEqualTo("object");
                assertThat(rootProperties).containsExactly("events");
                assertThat(schema.path("required").get(0).asText()).isEqualTo("events");
                assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
                assertThat(schema.path("properties").path("events").path("type").asText()).isEqualTo("array");

                JsonNode eventItem = schema.path("properties").path("events").path("items");
                assertThat(eventItem.path("type").asText()).isEqualTo("object");
                assertThat(eventItem.path("additionalProperties").asBoolean()).isFalse();
                assertThat(eventItem.toString()).doesNotContain("anyOf", "format", "minimum", "maximum", "maxLength");

                Set<String> eventProperties = new HashSet<>();
                eventItem.path("properties").fieldNames().forEachRemaining(eventProperties::add);
                Set<String> expectedProperties = Set.of("type", "eventTime", "confidence", "subject", "durationMinutes",
                        "quantity", "unit", "description", "calories", "activityType", "count", "company", "role",
                        "status", "mood", "notes", "applicationCount");
                assertThat(eventProperties).isEqualTo(expectedProperties);

                Set<String> requiredProperties = new HashSet<>();
                eventItem.path("required").forEach(node -> requiredProperties.add(node.asText()));
                assertThat(requiredProperties).isEqualTo(expectedProperties);

                JsonNode typeSchema = eventItem.path("properties").path("type");
                assertThat(typeSchema.path("type").asText()).isEqualTo("string");
                Set<String> eventTypes = new HashSet<>();
                typeSchema.path("enum").forEach(node -> eventTypes.add(node.asText()));
                assertThat(eventTypes).containsExactlyInAnyOrder(
                        "SLEEP", "WATER", "FOOD", "STUDY", "WORKOUT", "STEPS", "JOB_APPLICATION", "MOOD");

                for (String nullableField : expectedProperties) {
                    if (!"type".equals(nullableField)) {
                        assertThat(eventItem.path("properties").path(nullableField).path("type").toString())
                                .as("%s supports null for unknown values", nullableField)
                                .contains("null");
                    }
                }
                assertThat(eventItem.path("properties").path("unit").path("enum").toString())
                        .contains("GLASS", "ML", "LITER", "null");
        }
}
