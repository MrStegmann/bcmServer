package com.basketapp.bcmserver.domain.exception;

public class GameNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public GameNotFoundException(Long id) {
        super("Game with ID " + id + " was not found");
    }
}

