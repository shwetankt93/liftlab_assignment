package com.liftlab.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of event processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventProcessingResult {
    
    private boolean success;
    private String message;
    private Instant processedAt;
    
    public static EventProcessingResult success() {
        return EventProcessingResult.builder()
            .success(true)
            .message("Event processed successfully")
            .processedAt(Instant.now())
            .build();
    }
    
    public static EventProcessingResult failure(String message) {
        return EventProcessingResult.builder()
            .success(false)
            .message(message)
            .processedAt(Instant.now())
            .build();
    }
}

