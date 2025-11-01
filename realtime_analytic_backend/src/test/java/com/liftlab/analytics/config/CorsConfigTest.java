package com.liftlab.analytics.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CorsConfig
 */
class CorsConfigTest {

    @Test
    void testCorsWebFilterBean() {
        // Given
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173,http://localhost:3000");

        // When
        CorsWebFilter filter = corsConfig.corsWebFilter();

        // Then
        assertThat(filter).isNotNull();
    }

    @Test
    void testCorsConfigurationWithDefaultOrigins() {
        // Given
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173,http://localhost:3000,http://localhost:8081");

        // When
        CorsWebFilter filter = corsConfig.corsWebFilter();

        // Then
        assertThat(filter).isNotNull();
    }
}

