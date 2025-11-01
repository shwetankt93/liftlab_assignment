package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates session ID
 */
@Component
@Slf4j
public class SessionIdValidation implements IValidation {
    
    private static final String SESSION_ID_PREFIX = "sess_";
    
    @Override
    public ValidationResult validate(AnalyticsEvent event) {
        String sessionId = event.getSessionId();
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ValidationResult.failure("Session ID cannot be null or empty");
        }
        
        // Validate format (should start with "sess_")
        if (!sessionId.startsWith(SESSION_ID_PREFIX)) {
            return ValidationResult.failure(
                String.format("Session ID must start with '%s': %s", SESSION_ID_PREFIX, sessionId)
            );
        }
        
        return ValidationResult.success();
    }
}

