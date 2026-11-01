package com.basketapp.bcmserver.domain.exception;

public class PlayerNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException(Long id) {
        super("Player with ID " + id + " was not found");
    }
}

