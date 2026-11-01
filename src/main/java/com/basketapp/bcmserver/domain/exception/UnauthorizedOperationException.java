package com.basketapp.bcmserver.domain.exception;

public class UnauthorizedOperationException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
