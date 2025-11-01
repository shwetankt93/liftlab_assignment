package com.liftlab.analytics.exception;

import com.liftlab.analytics.model.ErrorResponse;
import com.liftlab.analytics.validation.ValidationException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private ServerWebExchange mockExchange;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockExchange = mock(ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpRequest mockRequest = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.reactive.ServerHttpResponse mockResponse = mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        
        when(mockExchange.getRequest()).thenReturn(mockRequest);
        when(mockExchange.getResponse()).thenReturn(mockResponse);
        
        // Mock path - RequestPath interface is tricky to mock, so we'll test path separately or skip it
        try {
            org.springframework.http.server.RequestPath mockRequestPath = mock(org.springframework.http.server.RequestPath.class);
            when(mockRequest.getPath()).thenReturn(mockRequestPath);
            when(mockRequestPath.value()).thenReturn("/api/events");
        } catch (Exception e) {
            // If mocking fails, we'll test without path verification
        }
    }

    @Test
    void testHandleValidationException() {
        // Given
        ValidationException ex = new ValidationException("Invalid user ID format");

        // When
        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleValidationException(ex, mockExchange);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).contains("Invalid user ID format");
                    assertThat(response.getBody().getError()).isEqualTo("Validation Error");
                })
                .verifyComplete();
    }

    @Test
    void testHandleRateLimitException() {
        // Given
        RequestNotPermitted ex = mock(RequestNotPermitted.class);
        when(ex.getMessage()).thenReturn("Rate limit exceeded");

        // When
        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleRateLimitException(ex, mockExchange);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).contains("Rate limit exceeded");
                    assertThat(response.getBody().getError()).isEqualTo("Too Many Requests");
                })
                .verifyComplete();
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception ex = new RuntimeException("Unexpected error occurred");

        // When
        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleGenericException(ex, mockExchange);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
                    assertThat(response.getBody().getMessage()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void testErrorResponseContainsTimestamp() {
        // Given
        ValidationException ex = new ValidationException("Test error");

        // When
        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleValidationException(ex, mockExchange);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getBody().getTimestamp()).isNotNull();
                    assertThat(response.getBody().getTimestamp()).isBefore(Instant.now().plusSeconds(1));
                })
                .verifyComplete();
    }

    @Test
    void testErrorResponseContainsPath() {
        // Given
        ValidationException ex = new ValidationException("Test error");
        // Setup path mock properly
        org.springframework.http.server.reactive.ServerHttpRequest mockRequest = mockExchange.getRequest();
        org.springframework.http.server.RequestPath mockRequestPath = mock(org.springframework.http.server.RequestPath.class);
        when(mockRequest.getPath()).thenReturn(mockRequestPath);
        when(mockRequestPath.value()).thenReturn("/api/events");

        // When
        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleValidationException(ex, mockExchange);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getBody().getPath()).isNotNull();
                })
                .verifyComplete();
    }
}

