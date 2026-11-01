package com.basketapp.bcmserver.domain.exception;

import java.util.Collections;
import java.util.Map;

public class SemanticValidationException extends DomainException {

    private static final long serialVersionUID = 1L;
    private final transient Map<String, String> invalidParams;

    public SemanticValidationException(String message) {
        super(message);
        this.invalidParams = Collections.emptyMap();
    }

    public SemanticValidationException(String message, Map<String, String> invalidParams) {
        super(message);
        this.invalidParams = invalidParams != null ? invalidParams : Collections.emptyMap();
    }

    public Map<String, String> getInvalidParams() {
        return invalidParams;
    }
}
