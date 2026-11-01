package com.basketapp.bcmserver.domain.exception;

public class UserAlreadyExistsException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final String username;

    public UserAlreadyExistsException(String username) {
        super("User with username '" + username + "' already exists");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
