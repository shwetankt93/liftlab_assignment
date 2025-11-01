package com.liftlab.analytics.exception;

import com.liftlab.analytics.model.ErrorResponse;
import com.liftlab.analytics.validation.ValidationException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Global exception handler
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .error("Validation Error")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(exchange.getRequest().getPath().value())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }
    
    @ExceptionHandler(RequestNotPermitted.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRateLimitException(
            RequestNotPermitted ex, ServerWebExchange exchange) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .error("Too Many Requests")
            .message("Rate limit exceeded. Please try again later.")
            .timestamp(Instant.now())
            .path(exchange.getRequest().getPath().value())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBindingException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        log.warn("Binding error: {}", ex.getMessage());
        
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Invalid request");
        
        ErrorResponse error = ErrorResponse.builder()
            .error("Bad Request")
            .message(message)
            .timestamp(Instant.now())
            .path(exchange.getRequest().getPath().value())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .timestamp(Instant.now())
            .path(exchange.getRequest().getPath().value())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}

