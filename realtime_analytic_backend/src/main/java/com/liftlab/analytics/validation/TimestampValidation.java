package com.liftlab.analytics.validation;

import com.liftlab.analytics.model.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Validates event timestamp format
 * Contract format: "yyyy-MM-dd'T'HH:mm:ss'Z'" (e.g., "2024-03-15T14:30:00Z")
 */
@Component
@Slf4j
public class TimestampValidation implements IValidation {
    
    private static final DateTimeFormatter EXPECTED_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    
    @Override
    public ValidationResult validate(AnalyticsEvent event) {
        if (event.getTimestamp() == null) {
            return ValidationResult.failure("Timestamp cannot be null");
        }

        try {
            // Format the timestamp to check if it matches the expected format
            String formattedTimestamp = event.getTimestamp().atOffset(ZoneOffset.UTC)
                    .format(EXPECTED_FORMAT);
            log.debug("Timestamp validation passed: {}", formattedTimestamp);


        } catch (DateTimeException e) {
            return ValidationResult.failure(
                    "Timestamp format is invalid. Expected format: yyyy-MM-dd'T'HH:mm:ss'Z' (e.g., 2024-03-15T14:30:00Z)"
            );
        }
        return ValidationResult.success();
    }
}

