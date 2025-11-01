package com.liftlab.analytics.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UrlNormalizer
 */
class UrlNormalizerTest {

    private UrlNormalizer urlNormalizer;

    @BeforeEach
    void setUp() {
        urlNormalizer = new UrlNormalizer();
    }

    @Test
    void testNormalizeSimpleUrl() {
        // Given
        String url = "/home";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home");
    }

    @Test
    void testNormalizeUrlWithQueryParameters() {
        // Given
        String url = "/home?param=value&other=123";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home");
    }

    @Test
    void testNormalizeUrlWithHashFragment() {
        // Given
        String url = "/home#section";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home");
    }

    @Test
    void testNormalizeUrlWithQueryAndHash() {
        // Given
        String url = "/home?param=value#section";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home");
    }

    @Test
    void testNormalizeUrlWithTrailingSlash() {
        // Given
        String url = "/home/";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home");
    }

    @Test
    void testNormalizeRootUrl() {
        // Given
        String url = "/";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("");
    }

    @Test
    void testNormalizeUrlToLowerCase() {
        // Given
        String url = "/HOME/PAGE";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home/page");
    }

    @Test
    void testNormalizeNullUrl() {
        // When
        String result = urlNormalizer.normalize(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testNormalizeEmptyUrl() {
        // When
        String result = urlNormalizer.normalize("");

        // Then
        assertThat(result).isEqualTo("");
    }

    @Test
    void testNormalizeUrlWithMultipleSlashes() {
        // Given
        String url = "/home//page/";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("home//page");
    }

    @Test
    void testNormalizeUrlRemovesLeadingSlash() {
        // Given
        String url = "/products/item/123";

        // When
        String result = urlNormalizer.normalize(url);

        // Then
        assertThat(result).isEqualTo("products/item/123");
        assertThat(result).doesNotStartWith("/");
    }
}

