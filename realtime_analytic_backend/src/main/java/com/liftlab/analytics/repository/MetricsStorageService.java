package com.liftlab.analytics.repository;

import com.liftlab.analytics.model.PageView;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Interface for metrics storage operations
 * Uses Strategy pattern - can swap implementations without code changes
 * All read methods return reactive types to avoid blocking operations
 */
public interface MetricsStorageService {
    
    // Active Users
    void recordActiveUser(String userId, long timestamp);
    Mono<Integer> getActiveUserCount(long windowStartTime);
    
    // Page Views
    void recordPageView(String pageUrl, long timestamp);
    Mono<Integer> getPageViewCount(String pageUrl, long windowStartTime);
    Mono<List<PageView>> getTopPages(int limit, long windowStartTime);
    
    // User Sessions
    void recordUserSession(String userId, String sessionId, long timestamp);
    Mono<Integer> getActiveSessionCount(String userId, long windowStartTime);
    Mono<Map<String, Integer>> getActiveSessionsByUser(long windowStartTime);
    
    // Cleanup methods - called when metrics are retrieved (returns Mono for reactive chaining)
    Mono<Void> cleanupActiveUsers(long currentTimestamp);
    Mono<Void> cleanupPageViews(long currentTimestamp);
    Mono<Void> cleanupUserSessions(long currentTimestamp);
}

