package com.basketapp.bcmserver.infrastructure.config;

import java.security.Principal;

/**
 * Principal object stored in Spring Security Context for authenticated users.
 */
public record AuthenticatedUser(Long id, String username) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}

