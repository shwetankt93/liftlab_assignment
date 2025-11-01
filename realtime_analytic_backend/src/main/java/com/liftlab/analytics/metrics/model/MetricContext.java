package com.liftlab.analytics.metrics.model;

import com.liftlab.analytics.repository.MetricsStorageService;
import lombok.Builder;
import lombok.Value;

/**
 * Context for metric calculations
 */
@Value
@Builder
public class MetricContext {
    long currentTimestamp;
    long fiveMinutesAgo;
    long fifteenMinutesAgo;
    MetricsStorageService storageService;
}

