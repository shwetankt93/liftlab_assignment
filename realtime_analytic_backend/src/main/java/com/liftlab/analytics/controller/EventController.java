package com.liftlab.analytics.controller;

import com.liftlab.analytics.model.AnalyticsEvent;
import com.liftlab.analytics.model.EventProcessingResult;
import com.liftlab.analytics.service.EventProcessingService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    
    private final EventProcessingService eventProcessingService;
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @RateLimiter(name = "eventIngestion")
    public Mono<ResponseEntity<EventProcessingResult>> ingestEvent(
            @RequestBody @Valid Mono<AnalyticsEvent> event) {
        
        return event
            .flatMap(eventProcessingService::processEvent)
            .map(ResponseEntity::ok)
            .onErrorResume(error -> {
                log.error("Error processing event", error);
                return Mono.just(ResponseEntity.ok(
                    EventProcessingResult.failure(error.getMessage())
                ));
            });
    }
}

