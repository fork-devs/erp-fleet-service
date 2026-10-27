package com.erp.fleet.domain.exception;

/**
 * Thrown when identity or vehicle validation cannot be performed because the
 * integration hub is temporarily unreachable or unavailable.
 *
 * <p>This exception signals that the entity should remain in PENDING status
 * and be rescheduled for an automated retry sweep.
 */
public class ValidationUnavailableException extends DomainException {

    public ValidationUnavailableException(String message) {
        super("FLEET_VALIDATION_UNAVAILABLE", message);
    }

    public ValidationUnavailableException(String message, Throwable cause) {
        super("FLEET_VALIDATION_UNAVAILABLE", message);
        initCause(cause);
    }
}
