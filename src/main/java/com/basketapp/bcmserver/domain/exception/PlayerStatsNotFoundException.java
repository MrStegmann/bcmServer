package com.basketapp.bcmserver.domain.exception;

public class PlayerStatsNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PlayerStatsNotFoundException(Long id) {
        super("Player stats with ID " + id + " was not found");
    }
}

