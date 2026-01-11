package com.jobplatform.orchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a job operation cannot be performed.
 *
 * Used for invalid state transitions or business rule violations.
 * Returns HTTP 400 Bad Request status.
 *
 * @author Hima Kammachi
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class JobOperationException extends RuntimeException {

    public JobOperationException(String message) {
        super(message);
    }

    public JobOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
