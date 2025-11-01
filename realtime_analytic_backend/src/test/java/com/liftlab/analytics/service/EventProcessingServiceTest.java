package com.liftlab.analytics.service;

import com.liftlab.analytics.model.AnalyticsEvent;
import com.liftlab.analytics.model.EventProcessingResult;
import com.liftlab.analytics.repository.MetricsStorageService;
import com.liftlab.analytics.util.UrlNormalizer;
import com.liftlab.analytics.validation.ValidationManager;
import com.liftlab.analytics.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventProcessingService
 */
@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private ValidationManager validationManager;

    @Mock
    private MetricsStorageService storageService;

    @Mock
    private UrlNormalizer urlNormalizer;

    private EventProcessingService eventProcessingService;

    @BeforeEach
    void setUp() {
        eventProcessingService = new EventProcessingService(
            validationManager, 
            storageService, 
            urlNormalizer
        );
    }

    @Test
    void testProcessEventSuccess() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validationManager.validate(event)).thenReturn(ValidationResult.success());
        when(urlNormalizer.normalize("/home")).thenReturn("home");
        doNothing().when(storageService).recordActiveUser(anyString(), anyLong());
        doNothing().when(storageService).recordPageView(anyString(), anyLong());
        doNothing().when(storageService).recordUserSession(anyString(), anyString(), anyLong());

        // When
        StepVerifier.create(eventProcessingService.processEvent(event))
                .expectNextMatches(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getMessage()).isNotNull();
                    return true;
                })
                .verifyComplete();

        // Then
        verify(validationManager, times(1)).validate(event);
        verify(urlNormalizer, times(1)).normalize("/home");
        verify(storageService, times(1)).recordActiveUser(eq("user123"), anyLong());
        verify(storageService, times(1)).recordPageView(eq("home"), anyLong());
        verify(storageService, times(1)).recordUserSession(eq("user123"), eq("sess456"), anyLong());
    }

    @Test
    void testProcessEventValidationFailure() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validationManager.validate(event))
                .thenThrow(new com.liftlab.analytics.validation.ValidationException("Validation failed"));

        // When
        StepVerifier.create(eventProcessingService.processEvent(event))
                .expectNextMatches(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getMessage()).isEqualTo("Validation failed");
                    return true;
                })
                .verifyComplete();

        // Then
        verify(validationManager, times(1)).validate(event);
        verify(storageService, never()).recordActiveUser(anyString(), anyLong());
        verify(storageService, never()).recordPageView(anyString(), anyLong());
        verify(storageService, never()).recordUserSession(anyString(), anyString(), anyLong());
    }

    @Test
    void testProcessEventHandlesExceptions() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home")
                .eventType("page_view")
                .build();

        when(validationManager.validate(event))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        StepVerifier.create(eventProcessingService.processEvent(event))
                .expectNextMatches(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getMessage()).contains("Unexpected error");
                    return true;
                })
                .verifyComplete();

        // Then
        verify(validationManager, times(1)).validate(event);
        verify(storageService, never()).recordActiveUser(anyString(), anyLong());
    }

    @Test
    void testProcessEventNormalizesUrl() {
        // Given
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .sessionId("sess456")
                .pageUrl("/home?param=value#section")
                .eventType("page_view")
                .build();

        when(validationManager.validate(event)).thenReturn(ValidationResult.success());
        when(urlNormalizer.normalize("/home?param=value#section")).thenReturn("home");
        doNothing().when(storageService).recordActiveUser(anyString(), anyLong());
        doNothing().when(storageService).recordPageView(anyString(), anyLong());
        doNothing().when(storageService).recordUserSession(anyString(), anyString(), anyLong());

        // When
        StepVerifier.create(eventProcessingService.processEvent(event))
                .expectNextMatches(EventProcessingResult::isSuccess)
                .verifyComplete();

        // Then
        verify(urlNormalizer, times(1)).normalize(eq("/home?param=value#section"));
        verify(storageService, times(1)).recordPageView(eq("home"), anyLong());
    }
}

