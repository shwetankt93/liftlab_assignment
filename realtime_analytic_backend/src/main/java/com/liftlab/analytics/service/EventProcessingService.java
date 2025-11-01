package com.liftlab.analytics.service;

import com.liftlab.analytics.model.AnalyticsEvent;
import com.liftlab.analytics.model.EventProcessingResult;
import com.liftlab.analytics.repository.MetricsStorageService;
import com.liftlab.analytics.util.UrlNormalizer;
import com.liftlab.analytics.validation.ValidationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for processing analytics events
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventProcessingService {
    
    private final ValidationManager validationManager;
    private final MetricsStorageService storageService;
    private final UrlNormalizer urlNormalizer;
    
    public Mono<EventProcessingResult> processEvent(AnalyticsEvent event) {
        try {
            // Event Processing
            log.debug("Processing event: {}", event);
            
            // 1. Validate event
            validationManager.validate(event);
            
            // 2. Extract timestamp
            long eventTimestamp = event.getTimestamp().toEpochMilli();
            
            // 3. Process metrics (storage operations)
            // Active Users
            storageService.recordActiveUser(event.getUserId(), eventTimestamp);
            
            // Page Views
            String normalizedUrl = urlNormalizer.normalize(event.getPageUrl());
            storageService.recordPageView(normalizedUrl, eventTimestamp);
            
            // User Sessions
            storageService.recordUserSession(event.getUserId(), event.getSessionId(), eventTimestamp);
            
            log.debug("Event processed successfully: {}", event.getUserId());
            return Mono.just(EventProcessingResult.success());
            
        } catch (Exception e) {
            log.error("Error processing event", e);
            return Mono.just(EventProcessingResult.failure(e.getMessage()));
        }
    }
}

