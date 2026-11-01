package com.basketapp.bcmserver.domain.exception;

public class UserNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(Long id) {
        super("User with id " + id + " not found");
    }

    public UserNotFoundException(String username) {
        super("User with username '" + username + "' not found");
    }
}
