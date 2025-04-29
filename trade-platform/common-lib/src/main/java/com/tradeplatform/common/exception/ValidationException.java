package com.tradeplatform.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown when validation of input data fails.
 */
public class ValidationException extends TradePlatformException {

    /**
     * Map of field names to error messages.
     */
    private final Map<String, String> fieldErrors;

    /**
     * Constructs a new ValidationException with the specified detail message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = Collections.emptyMap();
    }

    /**
     * Constructs a new ValidationException with the specified detail message and field errors.
     *
     * @param message     the detail message
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    /**
     * Constructs a new ValidationException with the specified detail message, cause, and field errors.
     *
     * @param message     the detail message
     * @param cause       the cause
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(String message, Throwable cause, Map<String, String> fieldErrors) {
        super(message, cause, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    /**
     * Returns the map of field names to error messages.
     *
     * @return the field errors
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    /**
     * Returns whether there are any field errors.
     *
     * @return true if there are field errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}