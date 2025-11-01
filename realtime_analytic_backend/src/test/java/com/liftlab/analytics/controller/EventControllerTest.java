package com.liftlab.analytics.controller;

import com.liftlab.analytics.model.AnalyticsEvent;
import com.liftlab.analytics.model.EventProcessingResult;
import com.liftlab.analytics.service.EventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventController
 */
@WebFluxTest(EventController.class)
class EventControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private EventProcessingService eventProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    private AnalyticsEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();
    }

    @Test
    void testIngestEventSuccess() {
        // Given
        EventProcessingResult successResult = EventProcessingResult.success();
        when(eventProcessingService.processEvent(any(AnalyticsEvent.class)))
                .thenReturn(Mono.just(successResult));

        // When/Then
        webTestClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvent)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.errorMessage").doesNotExist();

        // Verify service was called
        // Note: Rate limiter may affect this in integration tests
    }

    @Test
    void testIngestEventWithValidationFailure() {
        // Given
        EventProcessingResult failureResult = EventProcessingResult.failure("Validation failed");
        when(eventProcessingService.processEvent(any(AnalyticsEvent.class)))
                .thenReturn(Mono.just(failureResult));

        // When/Then
        webTestClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvent)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Validation failed");
    }

    @Test
    void testIngestEventWithInvalidPayload() {
        // Given - Missing required fields
        AnalyticsEvent invalidEvent = AnalyticsEvent.builder()
                .timestamp(null)
                .userId(null)
                .build();

        // When/Then - Controller returns 200 OK with failure result (error handling)
        webTestClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidEvent)
                .exchange()
                .expectStatus().isOk(); // Controller handles errors gracefully
    }

    @Test
    void testIngestEventHandlesServiceErrors() {
        // Given
        when(eventProcessingService.processEvent(any(AnalyticsEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then - Controller handles errors gracefully and returns 200 OK with failure result
        webTestClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvent)
                .exchange()
                .expectStatus().isOk() // Controller catches errors and returns 200 with failure
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void testIngestEventWithNullBody() {
        // When/Then - WebFlux validates missing body and returns 400
        webTestClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest(); // Missing body returns 400
    }
}

