package com.liftlab.analytics.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate limiting configuration
 */
@Configuration
public class RateLimitingConfig {
    
    @Value("${resilience4j.ratelimiter.instances.eventIngestion.limitForPeriod:100}")
    private int limitForPeriod;
    
    @Bean
    public RateLimiter eventIngestionRateLimiter() {
        return RateLimiter.of("eventIngestion", 
            RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build());
    }
}

