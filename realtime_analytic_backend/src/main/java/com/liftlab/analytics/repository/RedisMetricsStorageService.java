package com.liftlab.analytics.repository;

import com.liftlab.analytics.model.PageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis implementation of MetricsStorageService
 * Uses Redis ZSET for time-windowed metrics with TTL-based cleanup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMetricsStorageService implements MetricsStorageService {
    
    private static final String ACTIVE_USERS_KEY = "active_users:5m";
    private static final String PAGE_VIEWS_COUNTS_KEY = "page_views:counts";
    private static final String USERS_WITH_SESSIONS_KEY = "users_with_sessions:5m";
    private static final String PAGE_VIEW_PREFIX = "page_views:";
    private static final String USER_SESSION_PREFIX = "user_sessions:";
    private static final String USER_SESSION_SUFFIX = ":5m";
    
    private static final long ACTIVE_USERS_TTL_SECONDS = 600; // 10 minutes
    private static final long PAGE_VIEWS_TTL_SECONDS = 1800; // 30 minutes
    private static final long USER_SESSIONS_TTL_SECONDS = 600; // 10 minutes
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Override
    public void recordActiveUser(String userId, long timestamp) {
        // Just store the data - cleanup happens when metrics are retrieved
        redisTemplate.opsForZSet().add(ACTIVE_USERS_KEY, userId, (double) timestamp)
            .then(redisTemplate.expire(ACTIVE_USERS_KEY, Duration.ofSeconds(ACTIVE_USERS_TTL_SECONDS)))
            .subscribe(
                result -> log.debug("Recorded active user: {} at {}", userId, timestamp),
                error -> log.error("Error recording active user", error)
            );
    }
    
    @Override
    public Mono<Integer> getActiveUserCount(long windowStartTime) {
        Range<Double> range = Range.of(Range.Bound.inclusive((double) windowStartTime), Range.Bound.unbounded());
        return redisTemplate.opsForZSet()
            .count(ACTIVE_USERS_KEY, range)
            .map(Long::intValue)
            .defaultIfEmpty(0);
    }
    
    @Override
    public void recordPageView(String pageUrl, long timestamp) {
        // pageUrl is already normalized by UrlNormalizer in EventProcessingService
        // Just store the data - cleanup happens when metrics are retrieved
        String pageKey = PAGE_VIEW_PREFIX + pageUrl;
        String member = timestamp + ":" + UUID.randomUUID().toString().substring(0, 8);
        
        // Store page view and update count in master ranking
        redisTemplate.opsForZSet().add(pageKey, member, (double) timestamp)
            .then(redisTemplate.expire(pageKey, Duration.ofSeconds(PAGE_VIEWS_TTL_SECONDS)))
            .then(redisTemplate.opsForZSet().size(pageKey))
            .flatMap(count -> {
                // Update master ranking with current count (will be cleaned up during metrics retrieval)
                return redisTemplate.opsForZSet()
                    .add(PAGE_VIEWS_COUNTS_KEY, pageUrl, count.doubleValue())
                    .then(redisTemplate.expire(PAGE_VIEWS_COUNTS_KEY, Duration.ofSeconds(PAGE_VIEWS_TTL_SECONDS)));
            })
            .subscribe(
                result -> log.debug("Recorded page view: {} at {}", pageUrl, timestamp),
                error -> log.error("Error recording page view", error)
            );
    }
    
    @Override
    public Mono<Integer> getPageViewCount(String pageUrl, long windowStartTime) {
        // pageUrl is already normalized
        String pageKey = PAGE_VIEW_PREFIX + pageUrl;
        Range<Double> range = Range.of(Range.Bound.inclusive((double) windowStartTime), Range.Bound.unbounded());
        
        return redisTemplate.opsForZSet()
            .count(pageKey, range)
            .map(Long::intValue)
            .defaultIfEmpty(0);
    }
    
    @Override
    public Mono<List<PageView>> getTopPages(int limit, long windowStartTime) {
        // Get top pages from master ranking
        // Use Range<Long> for indices
        Range<Long> indexRange = Range.of(Range.Bound.inclusive(0L), Range.Bound.inclusive((long) (limit - 1)));
        
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(PAGE_VIEWS_COUNTS_KEY, indexRange)
            .flatMap(tuple -> {
                String url = tuple.getValue();
                String pageKey = PAGE_VIEW_PREFIX + url;
                
                // Validate count using reactive chain
                Range<Double> range = Range.of(Range.Bound.inclusive((double) windowStartTime), Range.Bound.unbounded());
                return redisTemplate.opsForZSet()
                    .count(pageKey, range)
                    .defaultIfEmpty(0L)
                    .flatMap(actualCount -> {
                        if (actualCount > 0) {
                            // URL is stored without leading slash, add it back for display
                            String displayUrl = url.startsWith("/") ? url : "/" + url;
                            return Mono.just(PageView.builder()
                                .url(displayUrl)
                                .viewCount(actualCount)
                                .build());
                        } else {
                            // Remove stale entry from master ranking (fire and forget)
                            redisTemplate.opsForZSet().remove(PAGE_VIEWS_COUNTS_KEY, url)
                                .subscribe(
                                    removed -> log.debug("Removed stale page from ranking: {}", url),
                                    error -> log.warn("Error removing stale page from ranking: {}", url, error)
                                );
                            return Mono.empty();
                        }
                    });
            })
            .collectList()
            .map(result -> {
                // Sort by count descending
                result.sort((a, b) -> Long.compare(b.getViewCount(), a.getViewCount()));
                return result.stream().limit(limit).collect(Collectors.toList());
            })
            .defaultIfEmpty(Collections.emptyList());
    }
    
    @Override
    public void recordUserSession(String userId, String sessionId, long timestamp) {
        // Just store the data - cleanup happens when metrics are retrieved
        String sessionKey = USER_SESSION_PREFIX + userId + USER_SESSION_SUFFIX;
        
        // Store session and track user in SET
        redisTemplate.opsForZSet().add(sessionKey, sessionId, (double) timestamp)
            .then(redisTemplate.opsForSet().add(USERS_WITH_SESSIONS_KEY, userId))
            .then(redisTemplate.expire(sessionKey, Duration.ofSeconds(USER_SESSIONS_TTL_SECONDS)))
            .then(redisTemplate.expire(USERS_WITH_SESSIONS_KEY, Duration.ofSeconds(USER_SESSIONS_TTL_SECONDS)))
            .subscribe(
                result -> log.debug("Recorded user session: {} - {} at {}", userId, sessionId, timestamp),
                error -> log.error("Error recording user session", error)
            );
    }
    
    @Override
    public Mono<Integer> getActiveSessionCount(String userId, long windowStartTime) {
        String sessionKey = USER_SESSION_PREFIX + userId + USER_SESSION_SUFFIX;
        Range<Double> range = Range.of(Range.Bound.inclusive((double) windowStartTime), Range.Bound.unbounded());
        
        return redisTemplate.opsForZSet()
            .count(sessionKey, range)
            .map(Long::intValue)
            .defaultIfEmpty(0);
    }
    
    @Override
    public Mono<Map<String, Integer>> getActiveSessionsByUser(long windowStartTime) {
        // Get all users with active sessions
        return redisTemplate.opsForSet()
            .members(USERS_WITH_SESSIONS_KEY)
            .collectList()
            .defaultIfEmpty(Collections.emptyList())
            .flatMapMany(Flux::fromIterable)
            .flatMap(userId -> {
                String sessionKey = USER_SESSION_PREFIX + userId + USER_SESSION_SUFFIX;
                
                // Check if key exists and get count in reactive chain
                return redisTemplate.hasKey(sessionKey)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return getActiveSessionCount(userId, windowStartTime)
                                .flatMap(count -> {
                                    if (count > 0) {
                                        return Mono.just(new AbstractMap.SimpleEntry<>(userId, count));
                                    } else {
                                        // Mark for removal (fire and forget)
                                        redisTemplate.opsForSet().remove(USERS_WITH_SESSIONS_KEY, userId)
                                            .subscribe(
                                                removed -> log.debug("Removed expired user from sessions set: {}", userId),
                                                error -> log.warn("Error removing expired user from sessions set: {}", userId, error)
                                            );
                                        return Mono.empty();
                                    }
                                });
                        } else {
                            // Key doesn't exist, mark for removal (fire and forget)
                            redisTemplate.opsForSet().remove(USERS_WITH_SESSIONS_KEY, userId)
                                .subscribe(
                                    removed -> log.debug("Removed expired user from sessions set: {}", userId),
                                    error -> log.warn("Error removing expired user from sessions set: {}", userId, error)
                                );
                            return Mono.empty();
                        }
                    });
            })
            .collectMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            )
            .defaultIfEmpty(Collections.emptyMap());
    }
    
    // ==================== CLEANUP METHODS - Called when metrics are retrieved ====================
    
    @Override
    public Mono<Void> cleanupActiveUsers(long currentTimestamp) {
        long fiveMinAgo = currentTimestamp - Duration.ofMinutes(5).toMillis();
        Range<Double> cleanupRange = Range.of(Range.Bound.unbounded(), Range.Bound.inclusive((double) fiveMinAgo));
        
        return redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_USERS_KEY, cleanupRange)
            .doOnNext(removed -> log.debug("Cleaned up {} old active user entries", removed))
            .doOnError(error -> log.error("Error cleaning up active users", error))
            .then();
    }
    
    @Override
    public Mono<Void> cleanupPageViews(long currentTimestamp) {
        long fifteenMinAgo = currentTimestamp - Duration.ofMinutes(15).toMillis();
        Range<Double> cleanupRange = Range.of(Range.Bound.unbounded(), Range.Bound.inclusive((double) fifteenMinAgo));
        
        // Use SCAN instead of KEYS to avoid blocking Redis
        // SCAN is non-blocking and cursor-based, suitable for production
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .match(PAGE_VIEW_PREFIX + "*")
            .count(100) // Process 100 keys at a time
            .build();
        
        return redisTemplate.scan(scanOptions)
            .flatMap(pageKey -> {
                return redisTemplate.opsForZSet().removeRangeByScore(pageKey, cleanupRange)
                    .flatMap(removed -> {
                        if (removed > 0) {
                            log.debug("Cleaned up {} entries from page key: {}", removed, pageKey);
                        }
                        // After cleanup, update the master ranking with actual count
                        String pageUrl = pageKey.substring(PAGE_VIEW_PREFIX.length());
                        return redisTemplate.opsForZSet().size(pageKey)
                            .flatMap(count -> {
                                if (count == 0) {
                                    // Remove from master ranking if no views left
                                    return redisTemplate.opsForZSet().remove(PAGE_VIEWS_COUNTS_KEY, pageUrl)
                                        .then(Mono.just(0L));
                                } else {
                                    // Update master ranking with actual count
                                    return redisTemplate.opsForZSet()
                                        .add(PAGE_VIEWS_COUNTS_KEY, pageUrl, count.doubleValue())
                                        .then(Mono.just(count));
                                }
                            });
                    });
            })
            .then()
            .doOnSuccess(result -> log.debug("Completed page views cleanup"))
            .doOnError(error -> log.error("Error during page views cleanup", error));
    }
    
    @Override
    public Mono<Void> cleanupUserSessions(long currentTimestamp) {
        long fiveMinAgo = currentTimestamp - Duration.ofMinutes(5).toMillis();
        Range<Double> cleanupRange = Range.of(Range.Bound.unbounded(), Range.Bound.inclusive((double) fiveMinAgo));
        
        // Use SCAN instead of KEYS to avoid blocking Redis
        // SCAN is non-blocking and cursor-based, suitable for production
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .match(USER_SESSION_PREFIX + "*" + USER_SESSION_SUFFIX)
            .count(100) // Process 100 keys at a time
            .build();
        
        return redisTemplate.scan(scanOptions)
            .flatMap(sessionKey -> {
                return redisTemplate.opsForZSet().removeRangeByScore(sessionKey, cleanupRange)
                    .flatMap(removed -> {
                        if (removed > 0) {
                            log.debug("Cleaned up {} entries from session key: {}", removed, sessionKey);
                        }
                        // Check if session key is now empty or expired
                        return redisTemplate.opsForZSet().size(sessionKey)
                            .flatMap(size -> {
                                if (size == 0) {
                                    // Extract userId from sessionKey: user_sessions:{userId}:5m
                                    String keyStr = sessionKey;
                                    String userId = keyStr.substring(USER_SESSION_PREFIX.length(), 
                                        keyStr.length() - USER_SESSION_SUFFIX.length());
                                    // Remove from users_with_sessions SET
                                    return redisTemplate.opsForSet().remove(USERS_WITH_SESSIONS_KEY, userId)
                                        .then(Mono.just(0L));
                                }
                                return Mono.just(size);
                            });
                    });
            })
            .then()
            .doOnSuccess(result -> log.debug("Completed user sessions cleanup"))
            .doOnError(error -> log.error("Error during user sessions cleanup", error));
    }
}

