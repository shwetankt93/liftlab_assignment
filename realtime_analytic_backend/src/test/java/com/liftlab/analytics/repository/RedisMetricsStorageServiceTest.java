package com.liftlab.analytics.repository;

import com.liftlab.analytics.model.PageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisMetricsStorageService using Testcontainers
 * TODO: Re-enable once Testcontainers Redis dependency issue is resolved
 */
@SpringBootTest
@Testcontainers
@Disabled("Temporarily disabled - requires Testcontainers Redis module")
class RedisMetricsStorageServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisMetricsStorageService storageService;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
    }

    @Test
    void testRecordAndGetActiveUser() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();
        String userId = "user123";

        // When
        storageService.recordActiveUser(userId, timestamp);

        // Wait for async operation to complete
        Thread.sleep(100);

        // Then
        int count = storageService.getActiveUserCount(timestamp - Duration.ofMinutes(5).toMillis()).block();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetActiveUserCountFiltersByTimeWindow() throws InterruptedException {
        // Given
        long currentTime = System.currentTimeMillis();
        long oldTime = currentTime - Duration.ofMinutes(10).toMillis(); // 10 minutes ago (outside 5min window)

        storageService.recordActiveUser("user1", currentTime);
        storageService.recordActiveUser("user2", oldTime);

        Thread.sleep(100);

        // When - cleanup old entries first
        storageService.cleanupActiveUsers(currentTime).block();

        // Then
        int count = storageService.getActiveUserCount(currentTime - Duration.ofMinutes(5).toMillis()).block();
        assertThat(count).isEqualTo(1); // Only user1 should be in window
    }

    @Test
    void testRecordAndGetPageView() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();
        String pageUrl = "home";

        // When
        storageService.recordPageView(pageUrl, timestamp);

        Thread.sleep(200); // Wait for async operations

        // Then
        int count = storageService.getPageViewCount(pageUrl, timestamp - Duration.ofMinutes(15).toMillis()).block();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetTopPages() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();

        storageService.recordPageView("home", timestamp);
        storageService.recordPageView("about", timestamp);
        storageService.recordPageView("home", timestamp); // Home gets 2 views

        Thread.sleep(300); // Wait for async operations

        // When
        List<PageView> topPages = storageService.getTopPages(10, timestamp - Duration.ofMinutes(15).toMillis()).block();

        // Then
        assertThat(topPages).isNotEmpty();
        assertThat(topPages.size()).isGreaterThanOrEqualTo(2);

        // Find home page
        PageView homePage = topPages.stream()
                .filter(p -> p.getUrl().contains("home"))
                .findFirst()
                .orElse(null);
        assertThat(homePage).isNotNull();
        assertThat(homePage.getViewCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testRecordAndGetUserSession() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();
        String userId = "user123";
        String sessionId = "sess456";

        // When
        storageService.recordUserSession(userId, sessionId, timestamp);

        Thread.sleep(100);

        // Then
        int count = storageService.getActiveSessionCount(userId, timestamp - Duration.ofMinutes(5).toMillis()).block();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetActiveSessionsByUser() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();

        storageService.recordUserSession("user1", "sess1", timestamp);
        storageService.recordUserSession("user1", "sess2", timestamp);
        storageService.recordUserSession("user2", "sess3", timestamp);

        Thread.sleep(200);

        // When
        Map<String, Integer> sessionsByUser = storageService.getActiveSessionsByUser(
                timestamp - Duration.ofMinutes(5).toMillis()).block();

        // Then
        assertThat(sessionsByUser).isNotEmpty();
        assertThat(sessionsByUser.get("user1")).isGreaterThanOrEqualTo(2);
        assertThat(sessionsByUser.get("user2")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testCleanupActiveUsers() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oldTime = currentTime - Duration.ofMinutes(10).toMillis();
        long recentTime = currentTime - Duration.ofMinutes(2).toMillis();

        storageService.recordActiveUser("oldUser", oldTime);
        storageService.recordActiveUser("recentUser", recentTime);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        StepVerifier.create(storageService.cleanupActiveUsers(currentTime))
                .verifyComplete();

        // Then
        int count = storageService.getActiveUserCount(currentTime - Duration.ofMinutes(5).toMillis()).block();
        assertThat(count).isEqualTo(1); // Only recentUser should remain
    }

    @Test
    void testCleanupPageViews() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oldTime = currentTime - Duration.ofMinutes(20).toMillis();
        long recentTime = currentTime - Duration.ofMinutes(5).toMillis();

        storageService.recordPageView("oldPage", oldTime);
        storageService.recordPageView("recentPage", recentTime);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        StepVerifier.create(storageService.cleanupPageViews(currentTime))
                .verifyComplete();

        // Then
        int oldCount = storageService.getPageViewCount("oldPage", currentTime - Duration.ofMinutes(15).toMillis()).block();
        int recentCount = storageService.getPageViewCount("recentPage", currentTime - Duration.ofMinutes(15).toMillis()).block();

        assertThat(oldCount).isEqualTo(0); // Old page should be cleaned up
        assertThat(recentCount).isGreaterThanOrEqualTo(1); // Recent page should remain
    }

    @Test
    void testCleanupUserSessions() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oldTime = currentTime - Duration.ofMinutes(10).toMillis();
        long recentTime = currentTime - Duration.ofMinutes(2).toMillis();

        storageService.recordUserSession("user1", "oldSess", oldTime);
        storageService.recordUserSession("user1", "recentSess", recentTime);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        StepVerifier.create(storageService.cleanupUserSessions(currentTime))
                .verifyComplete();

        // Then
        int count = storageService.getActiveSessionCount("user1", currentTime - Duration.ofMinutes(5).toMillis()).block();
        assertThat(count).isEqualTo(1); // Only recentSess should remain
    }

    @Test
    void testMultiplePageViewsForSameUrl() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();
        String pageUrl = "product";

        // When
        storageService.recordPageView(pageUrl, timestamp);
        storageService.recordPageView(pageUrl, timestamp + 1000);
        storageService.recordPageView(pageUrl, timestamp + 2000);

        Thread.sleep(400);

        // Then
        int count = storageService.getPageViewCount(pageUrl, timestamp - Duration.ofMinutes(15).toMillis()).block();
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    void testTopPagesOrdering() throws InterruptedException {
        // Given
        long timestamp = System.currentTimeMillis();

        // Record different page views with different counts
        storageService.recordPageView("low", timestamp);
        storageService.recordPageView("high", timestamp);
        storageService.recordPageView("high", timestamp);
        storageService.recordPageView("high", timestamp);
        storageService.recordPageView("medium", timestamp);
        storageService.recordPageView("medium", timestamp);

        Thread.sleep(500);

        // Cleanup and update rankings
        storageService.cleanupPageViews(System.currentTimeMillis()).block();

        // When
        List<PageView> topPages = storageService.getTopPages(3, timestamp - Duration.ofMinutes(15).toMillis()).block();

        // Then
        assertThat(topPages).isNotEmpty();
        // The top page should be "high" with the most views
        if (!topPages.isEmpty()) {
            PageView topPage = topPages.get(0);
            assertThat(topPage.getViewCount()).isGreaterThanOrEqualTo(3);
        }
    }
}

