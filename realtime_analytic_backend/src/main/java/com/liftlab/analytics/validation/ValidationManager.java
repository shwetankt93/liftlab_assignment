package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manages all validation implementations
 */
@Component
@Slf4j
public class ValidationManager {
    
    private final List<IValidation> validations;
    
    public ValidationManager(List<IValidation> validations) {
        this.validations = validations;
        log.info("Initialized ValidationManager with {} validations", validations.size());
    }
    
    /**
     * Validates an event using all registered validations
     * 
     * @param event The event to validate
     * @return ValidationResult
     * @throws ValidationException if validation fails
     */
    public ValidationResult validate(AnalyticsEvent event) {
        log.debug("Validating event: {}", event);
        
        for (IValidation validation : validations) {
            ValidationResult result = validation.validate(event);
            
            if (!result.isValid()) {
                log.warn("Validation failed: {} - {}", 
                    validation.getClass().getSimpleName(), result.getErrorMessage());
                throw new ValidationException(result.getErrorMessage());
            }
        }
        
        log.debug("Event validation successful");
        return ValidationResult.success();
    }
}

