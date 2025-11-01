package com.liftlab.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for URL normalization
 */
@Component
@Slf4j
public class UrlNormalizer {
    
    /**
     * Normalizes a URL by:
     * - Removing query parameters
     * - Removing hash fragments
     * - Removing trailing slashes (except root)
     * - Converting to lowercase
     * 
     * @param url The URL to normalize
     * @return Normalized URL
     */
    public String normalize(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        String normalized = url;
        
        // Remove query parameters
        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }
        
        // Remove hash fragments
        int hashIndex = normalized.indexOf('#');
        if (hashIndex != -1) {
            normalized = normalized.substring(0, hashIndex);
        }
        
        // Remove trailing slash (except root "/")
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        // Convert to lowercase for consistent matching
        normalized = normalized.toLowerCase();
        
        // Remove leading slash for storage (we'll add it back when retrieving)
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        
        log.debug("Normalized URL: {} -> {}", url, normalized);
        
        return normalized;
    }
}

