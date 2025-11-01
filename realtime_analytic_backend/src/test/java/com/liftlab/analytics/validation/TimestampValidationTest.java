package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TimestampValidation
 */
class TimestampValidationTest {

    private TimestampValidation timestampValidation;

    @BeforeEach
    void setUp() {
        timestampValidation = new TimestampValidation();
    }

    @Test
    void testValidateWithValidTimestamp() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = timestampValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void testValidateWithNullTimestamp() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(null)
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = timestampValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Timestamp cannot be null");
    }

    @Test
    void testValidateWithValidISO8601Format() {
        // Given
        Instant timestamp = Instant.parse("2024-03-15T14:30:00Z");
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(timestamp)
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = timestampValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithCurrentTimestamp() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = timestampValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithPastTimestamp() {
        // Given
        Instant pastTimestamp = Instant.now().minusSeconds(3600); // 1 hour ago
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(pastTimestamp)
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = timestampValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }
}

