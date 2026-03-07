package com.webapp.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 * Annotated with @ResponseStatus so Spring MVC returns 404 automatically.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() { super(); }
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String message, Throwable cause) { super(message, cause); }
}
