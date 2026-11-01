package com.basketapp.bcmserver.domain.exception;

public class TeamNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TeamNotFoundException(Long id) {
        super("Team with ID " + id + " was not found");
    }
}

