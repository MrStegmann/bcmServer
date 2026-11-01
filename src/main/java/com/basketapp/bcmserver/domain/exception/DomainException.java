package com.basketapp.bcmserver.domain.exception;

/**
 * Base abstract exception for all business domain rule violations.
 * Pure Java, free from external frameworks.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
