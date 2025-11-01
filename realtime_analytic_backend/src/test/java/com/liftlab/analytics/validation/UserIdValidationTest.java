package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserIdValidation
 */
class UserIdValidationTest {

    private UserIdValidation userIdValidation;

    @BeforeEach
    void setUp() {
        userIdValidation = new UserIdValidation();
    }

    @Test
    void testValidateWithValidUserId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("usr_123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = userIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithNullUserId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId(null)
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = userIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void testValidateWithEmptyUserId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = userIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateWithBlankUserId() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("   ")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = userIdValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }
}

