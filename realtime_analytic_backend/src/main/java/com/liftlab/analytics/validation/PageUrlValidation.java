package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates page URL
 */
@Component
@Slf4j
public class PageUrlValidation implements IValidation {
    
    @Override
    public ValidationResult validate(AnalyticsEvent event) {
        String pageUrl = event.getPageUrl();
        
        if (pageUrl == null || pageUrl.trim().isEmpty()) {
            return ValidationResult.failure("Page URL cannot be null or empty");
        }
        
        // Basic URL format validation - should start with "/"
        if (!pageUrl.startsWith("/")) {
            return ValidationResult.failure(
                String.format("Page URL must start with '/': %s", pageUrl)
            );
        }
        
        return ValidationResult.success();
    }
}

