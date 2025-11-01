package com.liftlab.analytics.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private boolean valid;
    private String errorMessage;
    
    public static ValidationResult success() {
        return ValidationResult.builder()
            .valid(true)
            .build();
    }
    
    public static ValidationResult failure(String errorMessage) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(errorMessage)
            .build();
    }
}

