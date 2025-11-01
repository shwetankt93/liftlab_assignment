package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PageUrlValidation
 */
class PageUrlValidationTest {

    private PageUrlValidation pageUrlValidation;

    @BeforeEach
    void setUp() {
        pageUrlValidation = new PageUrlValidation();
    }

    @Test
    void testValidateWithValidPageUrl() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = pageUrlValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithNullPageUrl() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl(null)
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = pageUrlValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateWithEmptyPageUrl() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = pageUrlValidation.validate(event);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateWithUrlWithQueryParams() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home?param=value")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = pageUrlValidation.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }
}

