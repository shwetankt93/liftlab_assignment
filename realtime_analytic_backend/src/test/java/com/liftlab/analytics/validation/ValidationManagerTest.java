package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidationManager
 */
@ExtendWith(MockitoExtension.class)
class ValidationManagerTest {

    @Mock
    private IValidation validation1;

    @Mock
    private IValidation validation2;

    @Mock
    private IValidation validation3;

    private ValidationManager validationManager;

    @BeforeEach
    void setUp() {
        List<IValidation> validations = Arrays.asList(validation1, validation2, validation3);
        validationManager = new ValidationManager(validations);
    }

    @Test
    void testValidateWithAllValidationsPassing() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validation1.validate(event)).thenReturn(ValidationResult.success());
        when(validation2.validate(event)).thenReturn(ValidationResult.success());
        when(validation3.validate(event)).thenReturn(ValidationResult.success());

        // When
        ValidationResult result = validationManager.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
        verify(validation1, times(1)).validate(event);
        verify(validation2, times(1)).validate(event);
        verify(validation3, times(1)).validate(event);
    }

    @Test
    void testValidateWithFirstValidationFailing() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("usr_123")
                .sessionId("sess_456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validation1.validate(event))
                .thenReturn(ValidationResult.failure("First validation failed"));

        // When/Then - ValidationManager throws ValidationException on failure
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            validationManager.validate(event);
        })
        .isInstanceOf(com.liftlab.analytics.validation.ValidationException.class)
        .hasMessageContaining("First validation failed");

        verify(validation1, times(1)).validate(event);
        verify(validation2, never()).validate(event);
        verify(validation3, never()).validate(event);
    }

    @Test
    void testValidateWithMiddleValidationFailing() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("usr_123")
                .sessionId("sess_456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validation1.validate(event)).thenReturn(ValidationResult.success());
        when(validation2.validate(event))
                .thenReturn(ValidationResult.failure("Second validation failed"));

        // When/Then - ValidationManager throws ValidationException on failure
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            validationManager.validate(event);
        })
        .isInstanceOf(com.liftlab.analytics.validation.ValidationException.class)
        .hasMessageContaining("Second validation failed");

        verify(validation1, times(1)).validate(event);
        verify(validation2, times(1)).validate(event);
        verify(validation3, never()).validate(event);
    }

    @Test
    void testValidateWithNoValidations() {
        // Given
        ValidationManager emptyManager = new ValidationManager(Collections.emptyList());
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        // When
        ValidationResult result = emptyManager.validate(event);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateStopsOnFirstFailure() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("usr_123")
                .sessionId("sess_456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validation1.validate(event))
                .thenReturn(ValidationResult.failure("Error 1"));

        // When/Then - ValidationManager throws ValidationException on failure
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            validationManager.validate(event);
        })
        .isInstanceOf(com.liftlab.analytics.validation.ValidationException.class)
        .hasMessageContaining("Error 1");

        verify(validation1, times(1)).validate(event);
        verify(validation2, never()).validate(event);
        verify(validation3, never()).validate(event);
    }
}

