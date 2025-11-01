package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates user ID
 */
@Component
@Slf4j
public class UserIdValidation implements IValidation {
    
    private static final String USER_ID_PREFIX = "usr_";
    
    @Override
    public ValidationResult validate(AnalyticsEvent event) {
        String userId = event.getUserId();
        
        if (userId == null || userId.trim().isEmpty()) {
            return ValidationResult.failure("User ID cannot be null or empty");
        }
        
        // Validate format (should start with "usr_")
        if (!userId.startsWith(USER_ID_PREFIX)) {
            return ValidationResult.failure(
                String.format("User ID must start with '%s': %s", USER_ID_PREFIX, userId)
            );
        }
        
        return ValidationResult.success();
    }
}

