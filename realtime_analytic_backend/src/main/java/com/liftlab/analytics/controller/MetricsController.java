package com.liftlab.analytics.controller;

import com.liftlab.analytics.model.MetricsResponse;
import com.liftlab.analytics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {
    
    private final MetricsService metricsService;
    
    @GetMapping
    public Mono<ResponseEntity<MetricsResponse>> getMetrics() {
        return metricsService.getCurrentMetrics()
            .map(ResponseEntity::ok)
            .onErrorResume(error -> {
                log.error("Error retrieving metrics", error);
                return Mono.just(ResponseEntity.ok(
                    MetricsResponse.builder()
                        .activeUsersCount(0)
                        .topPages(java.util.Collections.emptyList())
                        .activeSessionsByUser(java.util.Collections.emptyMap())
                        .timestamp(java.time.Instant.now())
                        .build()
                ));
            });
    }
}

