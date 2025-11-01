package com.liftlab.analytics.metrics.model;

/**
 * Result of metric calculation
 */
public interface MetricResult {
    String getMetricName();
    Object getValue();
}

