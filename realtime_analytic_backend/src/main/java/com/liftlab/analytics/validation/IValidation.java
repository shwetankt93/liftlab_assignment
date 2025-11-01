package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;

/**
 * Interface for event validation
 */
public interface IValidation {
    
    /**
     * Validates an analytics event
     * 
     * @param event The event to validate
     * @return ValidationResult containing validation status and error message if invalid
     */
    ValidationResult validate(AnalyticsEvent event);

}

