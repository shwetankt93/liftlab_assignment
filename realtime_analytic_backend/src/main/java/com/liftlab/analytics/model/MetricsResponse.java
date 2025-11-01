package com.liftlab.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Metrics API response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {
    
    private Integer activeUsersCount;
    private List<PageView> topPages;
    private Map<String, Integer> activeSessionsByUser;
    private Instant timestamp;
}

