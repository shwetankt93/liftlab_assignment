package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SessionIdValidation
 */
class SessionIdValidationTest {

    private SessionIdValidation sessionIdValidation;

    @BeforeEach
    void setUp() {
        sessionIdValidation = new SessionIdValidation();
    }

    @Test
    void testValidateWithValidSessionId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("usr_123")
                .sessionId("sess_456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = sessionIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithNullSessionId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId(null)
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = sessionIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateWithEmptySessionId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = sessionIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateWithBlankSessionId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("   ")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = sessionIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }
}

