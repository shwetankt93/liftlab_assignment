package com.liftlab.analytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Analytics Event DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    @NotBlank(message = "Page URL is required")
    private String pageUrl;
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
}

