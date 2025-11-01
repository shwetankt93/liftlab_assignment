package com.liftlab.analytics.kafka;

import com.liftlab.analytics.model.AnalyticsEvent;
import com.liftlab.analytics.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for analytics events
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventConsumer {
    
    private final EventProcessingService eventProcessingService;
    
    @KafkaListener(topics = "analytics-events", groupId = "analytics-consumer-group")
    public void consume(AnalyticsEvent event) {
        log.info("Received event from Kafka: {}", event);
        eventProcessingService.processEvent(event)
            .subscribe(
                result -> {
                    if (result.isSuccess()) {
                        log.debug("Event processed successfully from Kafka: {}", event.getUserId());
                    } else {
                        log.warn("Event processing failed from Kafka: {}", result.getMessage());
                    }
                },
                error -> log.error("Error processing event from Kafka", error)
            );
    }
}

