package com.basketapp.bcmserver.domain.exception;

public class InvalidCredentialsException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
